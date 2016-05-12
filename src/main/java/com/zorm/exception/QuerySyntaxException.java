package com.zorm.exception;

import antlr.RecognitionException;

public class QuerySyntaxException extends QueryException {

	public QuerySyntaxException(String message) {
		super( message );
	}

	public QuerySyntaxException(String message, String hql) {
		this( message );
		setQueryString( hql );
	}

	public static QuerySyntaxException convert(RecognitionException e) {
		return convert( e, null );
	}

	public static QuerySyntaxException convert(RecognitionException e, String hql) {
		String positionInfo = e.getLine() > 0 && e.getColumn() > 0
				? " near line " + e.getLine() + ", column " + e.getColumn()
				: "";
		return new QuerySyntaxException( e.getMessage() + positionInfo, hql );
	}

}
