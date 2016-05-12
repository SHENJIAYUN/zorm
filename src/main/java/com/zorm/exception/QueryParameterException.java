package com.zorm.exception;

public class QueryParameterException extends QueryException {

	public QueryParameterException(Exception e) {
		super( e );
	}

	public QueryParameterException(String message) {
		super( message );
	}

	public QueryParameterException(String message, Throwable e) {
		super( message, e );
	}

	public QueryParameterException(String message, String queryString) {
		super( message, queryString );
	}
}