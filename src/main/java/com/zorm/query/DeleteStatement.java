package com.zorm.query;

public class DeleteStatement extends AbstractRestrictableStatement {

	/**
	 * @see org.hibernate.hql.internal.ast.tree.Statement#getStatementType()
	 */
	public int getStatementType() {
		return SqlTokenTypes.DELETE;
	}

	/**
	 * @see org.hibernate.hql.internal.ast.tree.Statement#needsExecutor()
	 */
	public boolean needsExecutor() {
		return true;
	}

	@Override
    protected int getWhereClauseParentTokenType() {
		return SqlTokenTypes.FROM;
	}

}