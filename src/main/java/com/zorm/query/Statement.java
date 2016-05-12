package com.zorm.query;

public interface Statement {
	public SqlWalker getWalker();
	public int getStatementType();
	public boolean needsExecutor();
}
