package com.zorm.query;

import java.util.*;

import antlr.SemanticException;
import antlr.collections.AST;

import com.zorm.type.Type;
import com.zorm.util.ASTAppender;
import com.zorm.util.ASTIterator;

public class SelectClause extends SelectExpressionList {

	private boolean prepared = false;
	private boolean scalarSelect;

	private List fromElementsForLoad = new ArrayList();
	//private Type[] sqlResultTypes;
	private Type[] queryReturnTypes;
	private String[][] columnNames;
	private List collectionFromElements;
	private String[] aliases;
	private int[] columnNamesStartPositions;

	// Currently we can only have one...
	private AggregatedSelectExpression aggregatedSelectExpression;

	/**
	 * Does this SelectClause represent a scalar query
	 *
	 * @return True if this is a scalara select clause; false otherwise.
	 */
	public boolean isScalarSelect() {
		return scalarSelect;
	}

	public boolean isDistinct() {
		return getFirstChild() != null && getFirstChild().getType() == SqlTokenTypes.DISTINCT;
	}

	/**
	 * FromElements which need to be accounted for in the load phase (either for return or for fetch).
	 *
	 * @return List of appropriate FromElements.
	 */
	public List getFromElementsForLoad() {
		return fromElementsForLoad;
	}

	/*
	 * The types represented in the SQL result set.
	 *
	 * @return The types represented in the SQL result set.
	 */
	/*public Type[] getSqlResultTypes() {
		return sqlResultTypes;
	}*/

	/**
	 * The types actually being returned from this query at the "object level".
	 *
	 * @return The query return types.
	 */
	public Type[] getQueryReturnTypes() {
		return queryReturnTypes;
	}
	
	/**
	 * The HQL aliases, or generated aliases
	 *
	 * @return the aliases
	 */
	public String[] getQueryReturnAliases() {
		return aliases;
	}

	/**
	 * The column alias names being used in the generated SQL.
	 *
	 * @return The SQL column aliases.
	 */
	public String[][] getColumnNames() {
		return columnNames;
	}

	public int getColumnNamesStartPosition(int i) {
		return columnNamesStartPositions[ i ];
	}
	
	public static boolean VERSION2_SQL = false;

	protected AST getFirstSelectExpression() {
		AST n = getFirstChild();
		// Skip 'DISTINCT' and 'ALL', so we return the first expression node.
		while ( n != null && ( n.getType() == SqlTokenTypes.DISTINCT || n.getType() == SqlTokenTypes.ALL ) ) {
			n = n.getNextSibling();
		}
		return n;
	}

	public List getCollectionFromElements() {
		return collectionFromElements;
	}

	public AggregatedSelectExpression getAggregatedSelectExpression() {
		return aggregatedSelectExpression;
	}

	public void initializeDerivedSelectClause(FromClause  fromClause) throws SemanticException {
		if ( prepared ) {
			throw new IllegalStateException( "SelectClause was already prepared!" );
		}
		List fromElements = fromClause.getProjectionList();
		
		ASTAppender appender = new ASTAppender( getASTFactory(), this );	// Get ready to start adding nodes.
		int size = fromElements.size();
		ArrayList queryReturnTypeList = new ArrayList( size );

		Iterator iterator = fromElements.iterator();
		for ( int k = 0; iterator.hasNext(); k++ ) {
			FromElement fromElement = ( FromElement ) iterator.next();
			Type type = fromElement.getSelectType();

//			addCollectionFromElement( fromElement );

			if ( type != null ) {
				boolean collectionOfElements = fromElement.isCollectionOfValuesOrComponents();
				if ( !collectionOfElements ) {
					if ( !fromElement.isFetch() ) {
						// Add the type to the list of returned sqlResultTypes.
						queryReturnTypeList.add( type );
					}
					fromElementsForLoad.add( fromElement );
					// Generate the select expression.
					String text = fromElement.renderIdentifierSelect( size, k );
					SelectExpressionImpl generatedExpr = ( SelectExpressionImpl ) appender.append( SqlTokenTypes.SELECT_EXPR, text, false );
					if ( generatedExpr != null ) {
						generatedExpr.setFromElement( fromElement );
					}
				}
			}
		}

		// Get all the select expressions (that we just generated) and render the select.
		SelectExpression[] selectExpressions = collectSelectExpressions();

		if ( getWalker().isShallowQuery() ) {
			//renderScalarSelects( selectExpressions, fromClause );
		}
		else {
			renderNonScalarSelects( selectExpressions, fromClause );
		}
		finishInitialization( queryReturnTypeList );
	}
	
	private void finishInitialization(ArrayList queryReturnTypeList) {
		queryReturnTypes = ( Type[] ) queryReturnTypeList.toArray( new Type[queryReturnTypeList.size()] );
		initializeColumnNames();
		prepared = true;
	}
	
	private void initializeColumnNames() {
		// Generate an 2d array of column names, the first dimension is parallel with the
		// return types array.  The second dimension is the list of column names for each
		// type.

		// todo: we should really just collect these from the various SelectExpressions, rather than regenerating here
		columnNames = getSessionFactoryHelper().generateColumnNames( queryReturnTypes );
		columnNamesStartPositions = new int[ columnNames.length ];
		int startPosition = 1;
		for ( int i = 0 ; i < columnNames.length ; i ++ ) {
			columnNamesStartPositions[ i ] = startPosition;
			startPosition += columnNames[ i ].length;
		}
	}

	private void renderNonScalarSelects(SelectExpression[] selectExpressions, FromClause currentFromClause) 
			throws SemanticException {
				ASTAppender appender = new ASTAppender( getASTFactory(), this );
				final int size = selectExpressions.length;
				int nonscalarSize = 0;
				for ( int i = 0; i < size; i++ ) {
					if ( !selectExpressions[i].isScalar() ) nonscalarSize++;
				}

				int j = 0;
				for ( int i = 0; i < size; i++ ) {
					if ( !selectExpressions[i].isScalar() ) {
						SelectExpression expr = selectExpressions[i];
						FromElement fromElement = expr.getFromElement();
						if ( fromElement != null ) {
							renderNonScalarIdentifiers( fromElement, nonscalarSize, j, expr, appender );
							j++;
						}
					}
				}

				if ( !currentFromClause.isSubQuery() ) {
					// Generate the property select tokens.
					int k = 0;
					for ( int i = 0; i < size; i++ ) {
						if ( !selectExpressions[i].isScalar() ) {
							FromElement fromElement = selectExpressions[i].getFromElement();
							if ( fromElement != null ) {
								renderNonScalarProperties( appender, fromElement, nonscalarSize, k );
								k++;
							}
						}
					}
				}
			}
	
	private void renderNonScalarProperties(ASTAppender appender, FromElement fromElement, int nonscalarSize, int k) {
		String text = fromElement.renderPropertySelect( nonscalarSize, k );
		appender.append( SqlTokenTypes.SQL_TOKEN, text, false );
//		if ( fromElement.getQueryableCollection() != null && fromElement.isFetch() ) {
//			text = fromElement.renderCollectionSelectFragment( nonscalarSize, k );
//			appender.append( SqlTokenTypes.SQL_TOKEN, text, false );
//		}
		// Look through the FromElement's children to find any collections of values that should be fetched...
		ASTIterator iter = new ASTIterator( fromElement );
		while ( iter.hasNext() ) {
			FromElement child = ( FromElement ) iter.next();
//			if ( child.isCollectionOfValuesOrComponents() && child.isFetch() ) {
//				// Need a better way to define the suffixes here...
//				text = child.renderValueCollectionSelectFragment( nonscalarSize, nonscalarSize + k );
//				appender.append( SqlTokenTypes.SQL_TOKEN, text, false );
//			}
		}
	}
	
	private void renderNonScalarIdentifiers(FromElement fromElement, int nonscalarSize, int j, SelectExpression expr, ASTAppender appender) {
		String text = fromElement.renderIdentifierSelect( nonscalarSize, j );
		if ( !fromElement.getFromClause().isSubQuery() ) {
			if ( !scalarSelect && !getWalker().isShallowQuery() ) {
				//TODO: is this a bit ugly?
				expr.setText( text );
			}
			else {
				appender.append( SqlTokenTypes.SQL_TOKEN, text, false );
			}
		}
	}

}
