package com.zorm.query;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import antlr.ANTLRException;
import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

import com.zorm.engine.RowSelection;
import com.zorm.exception.MappingException;
import com.zorm.exception.QueryException;
import com.zorm.exception.QueryExecutionRequestException;
import com.zorm.exception.QuerySyntaxException;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.Queryable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImpl;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;
import com.zorm.util.ASTUtil;
import com.zorm.util.IdentitySet;
import com.zorm.util.ReflectHelper;

@SuppressWarnings("unused")
public class QueryTranslatorImpl implements QueryTranslator {
   private static final Log log = LogFactory.getLog(QueryTranslatorImpl.class);
 
   private SessionFactoryImplementor factory;
   
   private final String queryIdentifier;
   private String hql;
   private boolean compiled;
   private boolean shallowQuery;
   private QueryLoader queryLoader;
   private Map enabledFilters;
   private Map tokenReplacements;
   private Statement sqlAst;
   private String sql;
   private List collectedParameterSpecifications;
   private StatementExecutor statementExecutor;
   private ParameterTranslations paramTranslations;
   private Type[] actualReturnTypes;
   
	public QueryTranslatorImpl(
			String queryIdentifier,
	        String query,
	        Map enabledFilters,
	        SessionFactoryImplementor factory) {
		this.queryIdentifier = queryIdentifier;
		this.hql = query;
		this.compiled = false;
		this.shallowQuery = false;
		this.enabledFilters = enabledFilters;
		this.factory = factory;
	}

	@Override
	public void compile(Map replacements, boolean shallow)
			throws QueryException, MappingException {
		doCompile( replacements, shallow, null );
	}

	private synchronized void doCompile(Map replacements, boolean shallow, String collectionRole) {
		if(compiled){
			log.debug("compile() : The query is already compiled, skipping...");
			return;
		}
		this.tokenReplacements = replacements;
		if ( tokenReplacements == null ) {
			tokenReplacements = new HashMap();
		}
		this.shallowQuery = shallow;
		
		try{
		  // PHASE 1 : Parse the query into an AST.
		  QueryParser parser = parse( true );

		  // PHASE 2 : Analyze the AST, and produce an SQL AST.
		  SqlWalker w = analyze( parser, collectionRole );

		  sqlAst = ( Statement ) w.getAST();

		  if ( sqlAst.needsExecutor() ) {
				statementExecutor = buildAppropriateStatementExecutor( w );
		  }
		  else {
			// PHASE 3 : Generate the SQL.
			generate( ( QueryNode ) sqlAst );
			queryLoader = new QueryLoader( this, factory, w.getSelectClause() );
		  }

		compiled = true;
		}
		catch ( QueryException qe ) {
					qe.setQueryString( hql );
					throw qe;
				}
				catch ( RecognitionException e ) {
					// we do not actually propagate ANTLRExceptions as a cause, so
					// log it here for diagnostic purposes
					log.trace( "Converted antlr.RecognitionException", e );
					throw QuerySyntaxException.convert( e, hql );
				}
				catch ( ANTLRException e ) {
					// we do not actually propagate ANTLRExceptions as a cause, so
					// log it here for diagnostic purposes
					log.trace( "Converted antlr.ANTLRException", e );
					throw new QueryException( e.getMessage(), hql );
				}

				this.enabledFilters = null; 
	}
	
	private StatementExecutor buildAppropriateStatementExecutor(SqlWalker walker) {
		Statement statement = ( Statement ) walker.getAST();
		if ( walker.getStatementType() == SqlTokenTypes.DELETE ) {
			FromElement fromElement = walker.getFinalFromClause().getFromElement();
			Queryable persister = fromElement.getQueryable();
			if ( persister.isMultiTable() ) {
				//return new MultiTableDeleteExecutor( walker );
				return null;
			}
			else {
				return new BasicExecutor( walker, persister );
			}
		}
		else if ( walker.getStatementType() == SqlTokenTypes.UPDATE ) {
			FromElement fromElement = walker.getFinalFromClause().getFromElement();
			Queryable persister = fromElement.getQueryable();
			if ( persister.isMultiTable() ) {
//				return new MultiTableUpdateExecutor( walker );
				return null;
			}
			else {
				return new BasicExecutor( walker, persister );
			}
		}
		else if ( walker.getStatementType() == SqlTokenTypes.INSERT ) {
//			return new BasicExecutor( walker, ( ( InsertStatement ) statement ).getIntoClause().getQueryable() );
		    return null;
		}
		else {
			throw new QueryException( "Unexpected statement type" );
		}
	}

	private void generate(AST sqlAst) throws QueryException, RecognitionException {
		if ( sql == null ) {
			SqlGenerator gen = new SqlGenerator(factory);
			gen.statement( sqlAst );
			sql = gen.getSQL();
			gen.getParseErrorHandler().throwQueryException();
			collectedParameterSpecifications = gen.getCollectedParameters();
		}
	}
	
	private SqlWalker analyze(QueryParser parser, String collectionRole) throws QueryException, RecognitionException {
		SqlWalker w = new SqlWalker( this, factory, parser, tokenReplacements, collectionRole );
		AST hqlAst = parser.getAST();

		// Transform the tree.
		w.statement( hqlAst );

		w.getParseErrorHandler().throwQueryException();

		return w;
	}

	private QueryParser parse(boolean filter)  throws TokenStreamException, RecognitionException{
		QueryParser parser = QueryParser.getInstance(hql);
		parser.setFilter(filter);
		
		log.debug("parse() - query:"+hql);
		
		//创建查询语句的语法树
		parser.statement();
		
		AST hqlAst = parser.getAST();
		
		JavaConstantConverter converter = new JavaConstantConverter();
		NodeTraverser walker = new NodeTraverser( converter );
		//对语法树进行深度优先遍历
		walker.traverseDepthFirst( hqlAst );
		parser.getParseErrorHandler().throwQueryException();
		return parser;
	}

	public static class JavaConstantConverter implements NodeTraverser.VisitationStrategy {
		private AST dotRoot;
		public void visit(AST node) {
			if ( dotRoot != null ) {
				// we are already processing a dot-structure
                if (ASTUtil.isSubtreeChild(dotRoot, node)) return;
                // we are now at a new tree level
                dotRoot = null;
			}

			if ( dotRoot == null && node.getType() == TokenTypes.DOT ) {
				dotRoot = node;
				handleDotStructure( dotRoot );
			}
		}
		private void handleDotStructure(AST dotStructureRoot) {
			//遇到“.”时，使用深度优先获取到完整的text
			String expression = ASTUtil.getPathText( dotStructureRoot );
			Object constant = ReflectHelper.getConstantValue( expression );
			if ( constant != null ) {
				dotStructureRoot.setFirstChild( null );
				dotStructureRoot.setType( TokenTypes.JAVA_CONSTANT );
				dotStructureRoot.setText( expression );
			}
		}
	}

	@Override
	public Set getQuerySpaces() {
		return getWalker().getQuerySpaces();
	}
	
	private SqlWalker getWalker() {
		return sqlAst.getWalker();
	}
	
	public boolean isManipulationStatement() {
		return sqlAst.needsExecutor();
	}

	@Override
	public List<String> collectSqlStrings() {
		ArrayList<String> list = new ArrayList<String>();
		if ( isManipulationStatement() ) {
			String[] sqlStatements = statementExecutor.getSqlStatements();
			for ( int i = 0; i < sqlStatements.length; i++ ) {
				list.add( sqlStatements[i] );
			}
		}
		else {
			list.add( sql );
		}
		return list;
	}

	@Override
	public ParameterTranslations getParameterTranslations() {
		if ( paramTranslations == null ) {
			paramTranslations = new ParameterTranslationsImpl( getWalker().getParameters() );
//			paramTranslations = new ParameterTranslationsImpl( collectedParameterSpecifications );
		}
		return paramTranslations;
	}
	
	public Type[] getReturnTypes() {
		errorIfDML();
		return getWalker().getReturnTypes();
	}

	@Override
	public String[] getReturnAliases() {
		errorIfDML();
		return getWalker().getReturnAliases();
	}
	
	private void errorIfDML() throws ZormException {
		if ( sqlAst.needsExecutor() ) {
			throw new QueryExecutionRequestException( "Not supported for DML operations", hql );
		}
	}
	
	@Override
	public List list(SessionImplementor session, QueryParameters queryParameters) {
		// Delegate to the QueryLoader...
		errorIfDML();
		QueryNode query = ( QueryNode ) sqlAst;
		boolean hasLimit = queryParameters.getRowSelection() != null && queryParameters.getRowSelection().definesLimits();
		boolean needsDistincting = ( query.getSelectClause().isDistinct() || hasLimit ) && containsCollectionFetches();

		QueryParameters queryParametersToUse;
		if ( hasLimit && containsCollectionFetches() ) {
			RowSelection selection = new RowSelection();
			selection.setFetchSize( queryParameters.getRowSelection().getFetchSize() );
			selection.setTimeout( queryParameters.getRowSelection().getTimeout() );
			queryParametersToUse = queryParameters.createCopyUsing( selection );
		}
		else {
			queryParametersToUse = queryParameters;
		}

		List results = queryLoader.list( session, queryParametersToUse );

		if ( needsDistincting ) {
			int includedCount = -1;
			// NOTE : firstRow is zero-based
			int first = !hasLimit || queryParameters.getRowSelection().getFirstRow() == null
						? 0
						: queryParameters.getRowSelection().getFirstRow().intValue();
			int max = !hasLimit || queryParameters.getRowSelection().getMaxRows() == null
						? -1
						: queryParameters.getRowSelection().getMaxRows().intValue();
			int size = results.size();
			List tmp = new ArrayList();
			IdentitySet distinction = new IdentitySet();
			for ( int i = 0; i < size; i++ ) {
				final Object result = results.get( i );
				if ( !distinction.add( result ) ) {
					continue;
				}
				includedCount++;
				if ( includedCount < first ) {
					continue;
				}
				tmp.add( result );
				// NOTE : ( max - 1 ) because first is zero-based while max is not...
				if ( max >= 0 && ( includedCount - first ) >= ( max - 1 ) ) {
					break;
				}
			}
			results = tmp;
		}

		return results;
	}

	private boolean containsCollectionFetches() {
		return false;
	}

	public String getQueryString() {
		return hql;
	}

	public boolean isShallowQuery() {
		return shallowQuery;
	}

	public String getSQLString() {
		return sql;
	}
	
	private void errorIfSelect() throws ZormException {
		if ( !sqlAst.needsExecutor() ) {
			throw new QueryExecutionRequestException( "Not supported for select queries", hql );
		}
	}
	
	@Override
	public int executeUpdate(QueryParameters queryParameters,SessionImpl session) {
		errorIfSelect();
		return statementExecutor.execute( queryParameters, session );
	}
}
