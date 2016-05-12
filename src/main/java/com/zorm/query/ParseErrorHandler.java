package com.zorm.query;

import com.zorm.exception.QueryException;

public interface ParseErrorHandler extends ErrorReporter{
	int getErrorCount();
	void throwQueryException() throws QueryException;
}
