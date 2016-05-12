package com.zorm.util;

import java.util.StringTokenizer;

public final class ParserHelper {
	public static final String HQL_VARIABLE_PREFIX = ":";

	public static final String HQL_SEPARATORS = " \n\r\f\t,()=<>&|+-=/*'^![]#~\\";
	//NOTICE: no " or . since they are part of (compound) identifiers
	public static final String PATH_SEPARATORS = ".";

	public static boolean isWhitespace(String str) {
		return StringHelper.WHITESPACE.indexOf( str ) > -1;
	}

	private ParserHelper() {
		//cannot instantiate
	}

//	public static void parse(Parser p, String text, String seperators, QueryTranslatorImpl q) throws QueryException {
//		StringTokenizer tokens = new StringTokenizer( text, seperators, true );
//		p.start( q );
//		while ( tokens.hasMoreElements() ) p.token( tokens.nextToken(), q );
//		p.end( q );
//	}

}
