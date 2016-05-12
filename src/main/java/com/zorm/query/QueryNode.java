package com.zorm.query;

import antlr.SemanticException;

import com.zorm.util.ASTUtil;

public class QueryNode extends AbstractRestrictableStatement implements SelectExpression {

	@Override
	public int getStatementType() {
		return 0;
	}

	@Override
	public boolean needsExecutor() {
		return false;
	}

	@Override
	protected int getWhereClauseParentTokenType() {
		// TODO Auto-generated method stub
		return 0;
	}

	public final SelectClause getSelectClause() {
		return ( SelectClause ) ASTUtil.findTypeInChildren( this, SqlTokenTypes.SELECT_CLAUSE );
	}

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		
	}

	@Override
	public boolean isScalar() throws SemanticException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public FromElement getFromElement() {
		// TODO Auto-generated method stub
		return null;
	}

}
