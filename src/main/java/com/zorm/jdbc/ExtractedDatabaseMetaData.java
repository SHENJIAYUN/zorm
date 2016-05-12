package com.zorm.jdbc;

public interface ExtractedDatabaseMetaData {

	public enum SQLStateType{
		XOpen,
		SQL99,
		UNKOWN
	}
	
	public boolean doesDataDefinitionCauseTransactionCommit();

	public boolean supportsDataDefinitionInTransaction();

	//判断是否允许批量更新
	public boolean supportsBatchUpdates();

	public boolean supportsScrollableResults();

	public boolean supportsGetGeneratedKeys();
}
