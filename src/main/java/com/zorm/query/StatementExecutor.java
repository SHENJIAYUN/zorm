package com.zorm.query;

import com.zorm.exception.ZormException;
import com.zorm.session.SessionImplementor;

public interface StatementExecutor {
	public String[] getSqlStatements();
	
	public int execute(QueryParameters parameters, SessionImplementor session) throws ZormException;
}
