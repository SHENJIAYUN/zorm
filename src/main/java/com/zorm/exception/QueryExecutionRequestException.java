package com.zorm.exception;

public class QueryExecutionRequestException extends QueryException {

	public QueryExecutionRequestException(String message, String queryString) {
		super( message, queryString );
	}
}

