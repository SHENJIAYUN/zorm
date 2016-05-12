package com.zorm.persister.entity;

import java.util.*;

import com.zorm.dialect.Dialect;
import com.zorm.dialect.function.SQLFunctionRegistry;
import com.zorm.util.StringHelper;

public final class Template {

	private static final Set<String> KEYWORDS = new HashSet<String>();
	private static final Set<String> BEFORE_TABLE_KEYWORDS = new HashSet<String>();
	private static final Set<String> FUNCTION_KEYWORDS = new HashSet<String>();
	static {
		KEYWORDS.add("and");
		KEYWORDS.add("or");
		KEYWORDS.add("not");
		KEYWORDS.add("like");
		KEYWORDS.add("is");
		KEYWORDS.add("in");
		KEYWORDS.add("between");
		KEYWORDS.add("null");
		KEYWORDS.add("select");
		KEYWORDS.add("distinct");
		KEYWORDS.add("from");
		KEYWORDS.add("join");
		KEYWORDS.add("inner");
		KEYWORDS.add("outer");
		KEYWORDS.add("left");
		KEYWORDS.add("right");
		KEYWORDS.add("on");
		KEYWORDS.add("where");
		KEYWORDS.add("having");
		KEYWORDS.add("group");
		KEYWORDS.add("order");
		KEYWORDS.add("by");
		KEYWORDS.add("desc");
		KEYWORDS.add("asc");
		KEYWORDS.add("limit");
		KEYWORDS.add("any");
		KEYWORDS.add("some");
		KEYWORDS.add("exists");
		KEYWORDS.add("all");
		KEYWORDS.add("union");
		KEYWORDS.add("minus");

		BEFORE_TABLE_KEYWORDS.add("from");
		BEFORE_TABLE_KEYWORDS.add("join");

		FUNCTION_KEYWORDS.add("as");
		FUNCTION_KEYWORDS.add("leading");
		FUNCTION_KEYWORDS.add("trailing");
		FUNCTION_KEYWORDS.add("from");
		FUNCTION_KEYWORDS.add("case");
		FUNCTION_KEYWORDS.add("when");
		FUNCTION_KEYWORDS.add("then");
		FUNCTION_KEYWORDS.add("else");
		FUNCTION_KEYWORDS.add("end");
	}

	public static final String TEMPLATE = "$PlaceHolder$";

	private Template() {}
	
	public static String renderWhereStringTemplate(String sqlWhereString, Dialect dialect, SQLFunctionRegistry functionRegistry) {
		return renderWhereStringTemplate(sqlWhereString, TEMPLATE, dialect, functionRegistry);
	}
	
	public static String renderWhereStringTemplate(String sqlWhereString, String placeholder, Dialect dialect, SQLFunctionRegistry functionRegistry ) {

		// IMPL NOTE : The basic process here is to tokenize the incoming string and to iterate over each token
		//		in turn.  As we process each token, we set a series of flags used to indicate the type of context in
		// 		which the tokens occur.  Depending on the state of those flags we decide whether we need to qualify
		//		identifier references.

		String symbols = new StringBuilder()
				.append( "=><!+-*/()',|&`" )
				.append( StringHelper.WHITESPACE )
				.append( dialect.openQuote() )
				.append( dialect.closeQuote() )
				.toString();
		StringTokenizer tokens = new StringTokenizer( sqlWhereString, symbols, true );
		StringBuilder result = new StringBuilder();

		boolean quoted = false;
		boolean quotedIdentifier = false;
		boolean beforeTable = false;
		boolean inFromClause = false;
		boolean afterFromTable = false;

		boolean hasMore = tokens.hasMoreTokens();
		String nextToken = hasMore ? tokens.nextToken() : null;
		while ( hasMore ) {
			String token = nextToken;
			String lcToken = token.toLowerCase();
			hasMore = tokens.hasMoreTokens();
			nextToken = hasMore ? tokens.nextToken() : null;

			boolean isQuoteCharacter = false;

			if ( !quotedIdentifier && "'".equals(token) ) {
				quoted = !quoted;
				isQuoteCharacter = true;
			}

			if ( !quoted ) {
				boolean isOpenQuote;
				if ( "`".equals(token) ) {
					isOpenQuote = !quotedIdentifier;
					token = lcToken = isOpenQuote
							? Character.toString( dialect.openQuote() )
							: Character.toString( dialect.closeQuote() );
					quotedIdentifier = isOpenQuote;
					isQuoteCharacter = true;
				}
				else if ( !quotedIdentifier && ( dialect.openQuote()==token.charAt(0) ) ) {
					isOpenQuote = true;
					quotedIdentifier = true;
					isQuoteCharacter = true;
				}
				else if ( quotedIdentifier && ( dialect.closeQuote()==token.charAt(0) ) ) {
					quotedIdentifier = false;
					isQuoteCharacter = true;
					isOpenQuote = false;
				}
				else {
					isOpenQuote = false;
				}

				if ( isOpenQuote ) {
					result.append( placeholder ).append( '.' );
				}
			}

			// Special processing for ANSI SQL EXTRACT function
//			if ( "extract".equals( lcToken ) && "(".equals( nextToken ) ) {
//				final String field = extractUntil( tokens, "from" );
//				final String source = renderWhereStringTemplate(
//						extractUntil( tokens, ")" ),
//						placeholder,
//						dialect,
//						functionRegistry
//				);
//				result.append( "extract(" ).append( field ).append( " from " ).append( source ).append( ')' );
//
//				hasMore = tokens.hasMoreTokens();
//				nextToken = hasMore ? tokens.nextToken() : null;
//
//				continue;
//			}

			// Special processing for ANSI SQL TRIM function
			if ( "trim".equals( lcToken ) && "(".equals( nextToken ) ) {
				List<String> operands = new ArrayList<String>();
				StringBuilder builder = new StringBuilder();

				boolean hasMoreOperands = true;
				String operandToken = tokens.nextToken();
				boolean quotedOperand = false;
				while ( hasMoreOperands ) {
					final boolean isQuote = "'".equals( operandToken );
					if ( isQuote ) {
						quotedOperand = !quotedOperand;
						if ( !quotedOperand ) {
							operands.add( builder.append( '\'' ).toString() );
							builder.setLength( 0 );
						}
						else {
							builder.append( '\'' );
						}
					}
					else if ( quotedOperand ) {
						builder.append( operandToken );
					}
					else if ( operandToken.length() == 1 && Character.isWhitespace( operandToken.charAt( 0 ) ) ) {
						// do nothing
					}
					else {
						operands.add( operandToken );
					}
					operandToken = tokens.nextToken();
					hasMoreOperands = tokens.hasMoreTokens() && ! ")".equals( operandToken );
				}

//				TrimOperands trimOperands = new TrimOperands( operands );
//				result.append( "trim(" );
//				if ( trimOperands.trimSpec != null ) {
//					result.append( trimOperands.trimSpec ).append( ' ' );
//				}
//				if ( trimOperands.trimChar != null ) {
//					if ( trimOperands.trimChar.startsWith( "'" ) && trimOperands.trimChar.endsWith( "'" ) ) {
//						result.append( trimOperands.trimChar );
//					}
//					else {
//						result.append(
//								renderWhereStringTemplate( trimOperands.trimSpec, placeholder, dialect, functionRegistry )
//						);
//					}
//					result.append( ' ' );
//				}
//				if ( trimOperands.from != null ) {
//					result.append( trimOperands.from ).append( ' ' );
//				}
//				else if ( trimOperands.trimSpec != null || trimOperands.trimChar != null ) {
//					// I think ANSI SQL says that the 'from' is not optional if either trim-spec or trim-char are specified
//					result.append( "from " );
//				}
//
//				result.append( renderWhereStringTemplate( trimOperands.trimSource, placeholder, dialect, functionRegistry ) )
//						.append( ')' );

				hasMore = tokens.hasMoreTokens();
				nextToken = hasMore ? tokens.nextToken() : null;

				continue;
			}

			boolean quotedOrWhitespace = quoted || quotedIdentifier || isQuoteCharacter
					|| Character.isWhitespace( token.charAt(0) );

			if ( quotedOrWhitespace ) {
				result.append( token );
			}
			else if ( beforeTable ) {
				result.append( token );
				beforeTable = false;
				afterFromTable = true;
			}
			else if ( afterFromTable ) {
				if ( !"as".equals(lcToken) ) {
					afterFromTable = false;
				}
				result.append(token);
			}
//			else if ( isNamedParameter(token) ) {
//				result.append(token);
//			}
//			else if ( isIdentifier(token, dialect)
//					&& !isFunctionOrKeyword(lcToken, nextToken, dialect , functionRegistry) ) {
//				result.append(placeholder)
//						.append('.')
//						.append( dialect.quote(token) );
//			}
			else {
				if ( BEFORE_TABLE_KEYWORDS.contains(lcToken) ) {
					beforeTable = true;
					inFromClause = true;
				}
				else if ( inFromClause && ",".equals(lcToken) ) {
					beforeTable = true;
				}
				result.append(token);
			}

			//Yuck:
			if ( inFromClause
					&& KEYWORDS.contains( lcToken ) //"as" is not in KEYWORDS
					&& !BEFORE_TABLE_KEYWORDS.contains( lcToken ) ) {
				inFromClause = false;
			}
		}

		return result.toString();
	}

}
