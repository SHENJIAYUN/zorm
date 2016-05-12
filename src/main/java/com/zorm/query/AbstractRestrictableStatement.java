package com.zorm.query;

import com.zorm.util.ASTUtil;

import antlr.collections.AST;

public abstract class AbstractRestrictableStatement extends AbstractStatement implements RestrictableStatement {

	private FromClause fromClause;
	private AST whereClause;

	protected abstract int getWhereClauseParentTokenType();

	/**
	 * @see org.hibernate.hql.internal.ast.tree.RestrictableStatement#getFromClause
	 */
	public final FromClause getFromClause() {
		if ( fromClause == null ) {
			fromClause = ( FromClause ) ASTUtil.findTypeInChildren( this, SqlTokenTypes.FROM );
		}
		return fromClause;
	}

	/**
	 * @see RestrictableStatement#hasWhereClause
	 */
//	public final boolean hasWhereClause() {
//		AST whereClause = locateWhereClause();
//		return whereClause != null && whereClause.getNumberOfChildren() > 0;
//	}

	/**
	 * @see org.hibernate.hql.internal.ast.tree.RestrictableStatement#getWhereClause
	 */
//	public final AST getWhereClause() {
//		if ( whereClause == null ) {
//			whereClause = locateWhereClause();
//			// If there is no WHERE node, make one.
//			if ( whereClause == null ) {
//				getLog().debug( "getWhereClause() : Creating a new WHERE clause..." );
//				whereClause = ASTUtil.create( getWalker().getASTFactory(), HqlSqlTokenTypes.WHERE, "WHERE" );
//				// inject the WHERE after the parent
//				AST parent = ASTUtil.findTypeInChildren( this, getWhereClauseParentTokenType() );
//				whereClause.setNextSibling( parent.getNextSibling() );
//				parent.setNextSibling( whereClause );
//			}
//		}
//		return whereClause;
//	}

//	protected AST locateWhereClause() {
//		return ASTUtil.findTypeInChildren( this, TokenTypes.WHERE );
//	}
}