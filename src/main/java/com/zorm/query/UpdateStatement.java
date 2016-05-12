package com.zorm.query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.util.ASTUtil;

import antlr.collections.AST;

public class UpdateStatement extends AbstractRestrictableStatement{
  
	private static final long serialVersionUID = -7454992851152519918L;

    public int getStatementType() {
		return SqlTokenTypes.UPDATE;
	}

	/**
	 * @see org.hibernate.hql.internal.ast.tree.Statement#needsExecutor()
	 */
	public boolean needsExecutor() {
		return true;
	}

	@Override
  protected int getWhereClauseParentTokenType() {
		return SqlTokenTypes.SET;
	}

	public AST getSetClause() {
		return ASTUtil.findTypeInChildren( this, SqlTokenTypes.SET );
	}
}
