package com.zorm.query;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import antlr.ASTFactory;
import antlr.RecognitionException;
import antlr.SemanticException;
import antlr.collections.AST;

import com.zorm.exception.QueryException;
import com.zorm.persister.entity.Queryable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.sql.JoinType;
import com.zorm.type.Type;
import com.zorm.type.VersionType;
import com.zorm.util.ASTUtil;
import com.zorm.util.ArrayHelper;
import com.zorm.util.LiteralProcessor;
import com.zorm.util.SessionFactoryHelper;
import com.zorm.util.StringHelper;

public class SqlWalker extends SqlBaseWalker implements ErrorReporter, ParameterBinder.NamedParameterSource{

    private static final Log log = LogFactory.getLog(SqlWalker.class); 
	
	private final QueryTranslatorImpl queryTranslatorImpl;
	private final QueryParser hqlParser;
	private final SessionFactoryHelper sessionFactoryHelper;
	private final Map tokenReplacements;
	private final AliasGenerator aliasGenerator = new AliasGenerator();
	private final LiteralProcessor literalProcessor;
	private final ParseErrorHandler parseErrorHandler;
	private final ASTPrinter printer;
	private final String collectionFilterRole;

	private FromClause currentFromClause = null;
	private SelectClause selectClause;

	/**
	 * Maps each top-level result variable to its SelectExpression;
	 * (excludes result variables defined in subqueries)
	 **/
	private Map<String, SelectExpression> selectExpressionsByResultVariable = new HashMap();

	private Set querySpaces = new HashSet();

	private int parameterCount;
	private Map namedParameters = new HashMap();
	private ArrayList parameters = new ArrayList();
	private int numberOfParametersInSetClause;
	private int positionalParameterCount;

	private ArrayList assignmentSpecifications = new ArrayList();

	private JoinType impliedJoinType = JoinType.INNER_JOIN;

	/**
	 * Create a new tree transformer.
	 *
	 * @param qti Back pointer to the query translator implementation that is using this tree transform.
	 * @param sfi The session factory implementor where the Hibernate mappings can be found.
	 * @param parser A reference to the phase-1 parser
	 * @param tokenReplacements Registers the token replacement map with the walker.  This map will
	 * be used to substitute function names and constants.
	 * @param collectionRole The collection role name of the collection used as the basis for the
	 * filter, NULL if this is not a collection filter compilation.
	 */
	public SqlWalker(
			QueryTranslatorImpl qti,
			SessionFactoryImplementor sfi,
			QueryParser parser,
			Map tokenReplacements,
			String collectionRole) {
		setASTFactory( new SqlASTFactory( this ) );
		// Initialize the error handling delegate.
		this.parseErrorHandler = new ErrorCounter();
		this.queryTranslatorImpl = qti;
		this.sessionFactoryHelper = new SessionFactoryHelper( sfi );
		this.literalProcessor = new LiteralProcessor( this );
		this.tokenReplacements = tokenReplacements;
		this.collectionFilterRole = collectionRole;
		this.hqlParser = parser;
		this.printer = new ASTPrinter( SqlTokenTypes.class );
	}


	@Override
    protected void resolve(AST node) throws SemanticException {
		if ( node != null ) {
			// This is called when it's time to fully resolve a path expression.
			ResolvableNode r = ( ResolvableNode ) node;
			if ( isInFunctionCall() ) {
				r.resolveInFunctionCall( false, true );
			}
			else {
				r.resolve( false, true );	// Generate implicit joins, only if necessary.
			}
		}
	}
	
	@Override
	protected void processNumericLiteral(AST literal) throws SemanticException {
		literalProcessor.processNumeric( literal );
	}
	
	@Override
	protected void evaluateAssignment(AST eq) throws SemanticException {
		prepareLogicOperator( eq );
		Queryable persister = getCurrentFromClause().getFromElement().getQueryable();
		evaluateAssignment( eq, persister, -1 );
	}
	
	private void evaluateAssignment(AST eq, Queryable persister, int targetIndex) {
		if ( persister.isMultiTable() ) {
			// no need to even collect this information if the persister is considered multi-table
			AssignmentSpecification specification = new AssignmentSpecification( eq, persister );
			if ( targetIndex >= 0 ) {
				assignmentSpecifications.add( targetIndex, specification );
			}
			else {
				assignmentSpecifications.add( specification );
			}
			numberOfParametersInSetClause += specification.getParameters().length;
		}
	}
	
	@Override
	protected boolean isNonQualifiedPropertyRef(AST ident) {
		final String identText = ident.getText();
		if ( currentFromClause.isFromElementAlias( identText ) ) {
			return false;
		}

		List fromElements = currentFromClause.getExplicitFromElements();
		if ( fromElements.size() == 1 ) {
			final FromElement fromElement = ( FromElement ) fromElements.get( 0 );
			try {
				log.debug( "Attempting to resolve property ["+identText+"] as a non-qualified ref");
				return fromElement.getPropertyMapping( identText ).toType( identText ) != null;
			}
			catch( QueryException e ) {
				// Should mean that no such property was found
			}
		}

		return false;
	}
	
	@Override
	protected AST lookupNonQualifiedProperty(AST property)
			throws SemanticException {
		final FromElement fromElement = ( FromElement ) currentFromClause.getExplicitFromElements().get( 0 );
		AST syntheticDotNode = generateSyntheticDotNodeForNonQualifiedPropertyRef( property, fromElement );
		return lookupProperty( syntheticDotNode, false, getCurrentClauseType() == SqlTokenTypes.SELECT );
	}
	
	private AST generateSyntheticDotNodeForNonQualifiedPropertyRef(AST property, FromElement fromElement) {
		AST dot = getASTFactory().create( DOT, "{non-qualified-property-ref}" );
		// TODO : better way?!?
		( ( DotNode ) dot ).setPropertyPath( ( ( FromReferenceNode ) property ).getPath() );

		IdentNode syntheticAlias = ( IdentNode ) getASTFactory().create( IDENT, "{synthetic-alias}" );
		syntheticAlias.setFromElement( fromElement );
		syntheticAlias.setResolved();

		dot.setFirstChild( syntheticAlias );
		dot.addChild( property );

		return dot;
	}
	
	@Override
	protected AST lookupProperty(AST dot, boolean root, boolean inSelect)
			throws SemanticException {
		DotNode dotNode = ( DotNode ) dot;
		FromReferenceNode lhs = dotNode.getLhs();
		AST rhs = lhs.getNextSibling();
		switch ( rhs.getType() ) {
		case SqlTokenTypes.ELEMENTS:
		case SqlTokenTypes.INDICES:
//			CollectionFunction f = ( CollectionFunction ) rhs;
//			// Re-arrange the tree so that the collection function is the root and the lhs is the path.
//			f.setFirstChild( lhs );
//			lhs.setNextSibling( null );
//			dotNode.setFirstChild( f );
//			resolve( lhs );			// Don't forget to resolve the argument!
//			f.resolve( inSelect );	// Resolve the collection function now.
//			return f;
		default:
			// Resolve everything up to this dot, but don't resolve the placeholders yet.
			dotNode.resolveFirstChild();
			return dotNode;
	}
	}
	
	// handle trace logging ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private int traceDepth = 0;

	@Override
	public void traceIn(String ruleName, AST tree) {
	}

	@Override
	public void traceOut(String ruleName, AST tree) {
	}

	public boolean isFilter() {
		return collectionFilterRole != null;
	}

	public String getCollectionFilterRole() {
		return collectionFilterRole;
	}

	public SessionFactoryHelper getSessionFactoryHelper() {
		return sessionFactoryHelper;
	}

	public Map getTokenReplacements() {
		return tokenReplacements;
	}

	public AliasGenerator getAliasGenerator() {
		return aliasGenerator;
	}

	public FromClause getCurrentFromClause() {
		return currentFromClause;
	}

	public ParseErrorHandler getParseErrorHandler() {
		return parseErrorHandler;
	}

	@Override
    public void reportError(RecognitionException e) {
		parseErrorHandler.reportError( e ); // Use the delegate.
	}

	@Override
    public void reportError(String s) {
		parseErrorHandler.reportError( s ); // Use the delegate.
	}

	@Override
    public void reportWarning(String s) {
		parseErrorHandler.reportWarning( s );
	}

	/**
	 * Returns the set of unique query spaces (a.k.a.
	 * table names) that occurred in the query.
	 *
	 * @return A set of table names (Strings).
	 */
	public Set getQuerySpaces() {
		return querySpaces;
	}
	
	@Override
    protected void postProcessUpdate(AST update) throws SemanticException {
		UpdateStatement updateStatement = ( UpdateStatement ) update;

		postProcessDML( updateStatement );
	}

	private void postProcessDML(RestrictableStatement statement)throws SemanticException  {
		statement.getFromClause().resolve();
		FromElement fromElement = ( FromElement ) statement.getFromClause().getFromElements().get( 0 );
		Queryable persister = fromElement.getQueryable();
		fromElement.setText( persister.getTableName() );
	}


	@Override
    protected void prepareVersioned(AST updateNode, AST versioned) throws SemanticException {
		UpdateStatement updateStatement = ( UpdateStatement ) updateNode;
		FromClause fromClause = updateStatement.getFromClause();
		if ( versioned != null ) {
			// Make sure that the persister is versioned
//			Queryable persister = fromClause.getFromElement().getQueryable();
//			if ( !persister.isVersioned() ) {
//				throw new SemanticException( "increment option specified for update of non-versioned entity" );
//			}
//
//			VersionType versionType = persister.getVersionType();
////			if ( versionType instanceof UserVersionType ) {
////				throw new SemanticException( "user-defined version types not supported for increment option" );
////			}
//
//			AST eq = getASTFactory().create( SqlTokenTypes.EQ, "=" );
//			AST versionPropertyNode = generateVersionPropertyNode( persister );
//
//			eq.setFirstChild( versionPropertyNode );
//
//			AST versionIncrementNode = null;
//			if ( isTimestampBasedVersion( versionType ) ) {
//				versionIncrementNode = getASTFactory().create( HqlSqlTokenTypes.PARAM, "?" );
//				ParameterSpecification paramSpec = new VersionTypeSeedParameterSpecification( versionType );
//				( ( ParameterNode ) versionIncrementNode ).setHqlParameterSpecification( paramSpec );
//				parameters.add( 0, paramSpec );
//			}
//			else {
//				// Not possible to simply re-use the versionPropertyNode here as it causes
//				// OOM errors due to circularity :(
//				versionIncrementNode = getASTFactory().create( SqlTokenTypes.PLUS, "+" );
//				versionIncrementNode.setFirstChild( generateVersionPropertyNode( persister ) );
//				versionIncrementNode.addChild( getASTFactory().create( SqlTokenTypes.IDENT, "1" ) );
//			}
//
//			eq.addChild( versionIncrementNode );
//
//			evaluateAssignment( eq, persister, 0 );
//
//			AST setClause = updateStatement.getSetClause();
//			AST currentFirstSetElement = setClause.getFirstChild();
//			setClause.setFirstChild( eq );
//			eq.setNextSibling( currentFirstSetElement );
		}
	}

	/**
	 * Sets the current 'FROM' context.
	 *
	 * @param fromNode      The new 'FROM' context.
	 * @param inputFromNode The from node from the input AST.
	 */

	/**
	 * Returns to the previous 'FROM' context.
	 */
	private void popFromClause() {
		currentFromClause = currentFromClause.getParentFromClause();
	}

	public JoinType getImpliedJoinType() {
		return impliedJoinType;
	}

	private void trackNamedParameterPositions(String name) {
		Integer loc = parameterCount++;
		Object o = namedParameters.get( name );
		if ( o == null ) {
			namedParameters.put( name, loc );
		}
		else if ( o instanceof Integer ) {
			ArrayList list = new ArrayList( 4 );
			list.add( o );
			list.add( loc );
			namedParameters.put( name, list );
		}
		else {
			( ( ArrayList ) o ).add( loc );
		}
	}

	@Override
    protected boolean isOrderExpressionResultVariableRef(AST orderExpressionNode) throws SemanticException {
		// ORDER BY is not supported in a subquery
		// TODO: should an exception be thrown if an ORDER BY is in a subquery?
		if ( ! isSubQuery() &&
				orderExpressionNode.getType() == IDENT &&
				selectExpressionsByResultVariable.containsKey( orderExpressionNode.getText() ) ) {
			return true;
		}
		return false;
	}

	/**
	 * Returns the locations of all occurrences of the named parameter.
	 */
	public int[] getNamedParameterLocations(String name) throws QueryException {
		Object o = namedParameters.get( name );
		if ( o == null ) {
			QueryException qe = new QueryException( QueryTranslator.ERROR_NAMED_PARAMETER_DOES_NOT_APPEAR + name );
			qe.setQueryString( queryTranslatorImpl.getQueryString() );
			throw qe;
		}
		if ( o instanceof Integer ) {
			return new int[]{( ( Integer ) o ).intValue()};
		}
		else {
			return ArrayHelper.toIntArray( (ArrayList) o );
		}
	}

	public void addQuerySpaces(Serializable[] spaces) {
		querySpaces.addAll( Arrays.asList( spaces ) );
	}

	public Type[] getReturnTypes() {
		return selectClause.getQueryReturnTypes();
	}

	public String[] getReturnAliases() {
		return selectClause.getQueryReturnAliases();
	}

	public SelectClause getSelectClause() {
		return selectClause;
	}

	public FromClause getFinalFromClause() {
		FromClause top = currentFromClause;
		while ( top.getParentFromClause() != null ) {
			top = top.getParentFromClause();
		}
		return top;
	}

	
	public LiteralProcessor getLiteralProcessor() {
		return literalProcessor;
	}

	public ASTPrinter getASTPrinter() {
		return printer;
	}

	public ArrayList getParameters() {
		return parameters;
	}

	@Override
	protected void processIndex(AST indexOp) throws SemanticException {
//		IndexNode indexNode = ( IndexNode ) indexOp;
//		indexNode.resolve( true, true );
	}
	
	public int getNumberOfParametersInSetClause() {
		return numberOfParametersInSetClause;
	}
	
	@Override
    protected AST generatePositionalParameter(AST inputNode) throws SemanticException {
		if ( namedParameters.size() > 0 ) {
			throw new SemanticException( "cannot define positional parameter after any named parameters have been defined" );
		}
		ParameterNode parameter = ( ParameterNode ) astFactory.create( PARAM, "?" );
		PositionalParameterSpecification paramSpec = new PositionalParameterSpecification(
				inputNode.getLine(),
		        inputNode.getColumn(),
				positionalParameterCount++
		);
		parameter.setHqlParameterSpecification( paramSpec );
		parameters.add( paramSpec );
		return parameter;
	}

	public ArrayList getAssignmentSpecifications() {
		return assignmentSpecifications;
	}
	
	public static void panic() {
		throw new QueryException( "TreeWalker: panic" );
	}
	
	@Override
    protected AST createFromElement(String path, AST alias, AST propertyFetch) throws SemanticException {
		FromElement fromElement = currentFromClause.addFromElement( path, alias );
		fromElement.setAllPropertyFetch(propertyFetch!=null);
		return fromElement;
	}
	
	@Override
    protected void pushFromClause(AST fromNode, AST inputFromNode) {
		FromClause newFromClause = ( FromClause ) fromNode;
		newFromClause.setParentFromClause( currentFromClause );
		currentFromClause = newFromClause;
	}
	
	@Override
    protected void postProcessDelete(AST delete) throws SemanticException {
		postProcessDML( ( DeleteStatement ) delete );
	}
	
	@Override
	protected void processQuery(AST select, AST query) throws SemanticException {
		try{
			QueryNode qn = ( QueryNode ) query;
			boolean explicitSelect = select != null && select.getNumberOfChildren() > 0;
			if ( !explicitSelect ) {
				createSelectClauseFromFromClause( qn );
			}
			else{
//				useSelectClause( select );
			}
		}
		finally{
			popFromClause();
		}
	}


	private void createSelectClauseFromFromClause(QueryNode qn) throws SemanticException {
		AST select = astFactory.create( SELECT_CLAUSE, "{derived select clause}" );
		AST sibling = qn.getFromClause();
		qn.setFirstChild(select);
		select.setNextSibling(sibling);
		selectClause = (SelectClause) select;
		selectClause.initializeDerivedSelectClause( currentFromClause );
	}


	public boolean isShallowQuery() {
		return getStatementType() == INSERT || queryTranslatorImpl.isShallowQuery();
	}
	
	@Override
	protected void prepareLogicOperator(AST operator) throws SemanticException {
		( ( OperatorNode ) operator ).initialize();
	}
	
}
