package com.zorm.query;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.exception.MappingException;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.util.ParserHelper;
import com.zorm.util.StringHelper;

@SuppressWarnings("unchecked")
public final class QuerySplitter {

	private static final Log log = LogFactory.getLog(QuerySplitter.class);
	
	private static final Set BEFORE_CLASS_TOKENS = new HashSet();
	private static final Set NOT_AFTER_CLASS_TOKENS = new HashSet();

	static{
		BEFORE_CLASS_TOKENS.add("from");
		BEFORE_CLASS_TOKENS.add("delete");
		BEFORE_CLASS_TOKENS.add("update");
		BEFORE_CLASS_TOKENS.add(",");
		NOT_AFTER_CLASS_TOKENS.add("in");
		NOT_AFTER_CLASS_TOKENS.add("from");
		NOT_AFTER_CLASS_TOKENS.add(")");
	}
	
	private QuerySplitter(){}

	public static String[] concreteQueries(String query,SessionFactoryImplementor factory) throws MappingException{
		//将query语句拆分成一个个token
		String[] tokens = StringHelper.split( StringHelper.WHITESPACE + "(),", query, true );
		if(tokens.length==0) return new String[]{query};
		ArrayList placeholders = new ArrayList();
		ArrayList replacements = new ArrayList();
		StringBuilder templateQuery = new StringBuilder( 40 );
		//取得持久化类前的位置
		int start = getStartingPositionFor(tokens, templateQuery);
		int count = 1;
		String next = null;
		String last = tokens[start-1].toLowerCase();
		for(int i=start;i<tokens.length;i++){
			String token = tokens[i];
			if ( ParserHelper.isWhitespace( token ) ) {
				templateQuery.append( token );
				continue;
			}
			next = nextNonWhite(tokens,i).toLowerCase();
			//判断token是否可以作为Java标识符，以及是否可为类名
			boolean process = isJavaIdentifier( token ) &&
					isPossiblyClassName( last, next );
			last = token.toLowerCase();
			if(process){
				String importedClassName = getImportedClass( token, factory );
				if(importedClassName!=null){
					String[] implementors = factory.getImplementors( importedClassName );
					token = "$clazz" + count++ + "$";
					if ( implementors != null ) {
						placeholders.add( token );
						replacements.add( implementors );
					}
				}
			}
			templateQuery.append(token);
		}
		String[] results = StringHelper.multiply( templateQuery.toString(), placeholders.iterator(), replacements.iterator() );
		if ( results.length == 0 ) {
			log.warn("no persistent classes found for query class");;
		}
		return results;
	}

	private static String getImportedClass(String token,
			SessionFactoryImplementor factory) {
		return factory.getImportedClassName( token );
	}

	private static boolean isPossiblyClassName(String last, String next) {
		return "class".equals( last ) || (
				BEFORE_CLASS_TOKENS.contains( last ) &&
				!NOT_AFTER_CLASS_TOKENS.contains( next )
			);
	}

	//判断字符串能够成为Java的标识符
	private static boolean isJavaIdentifier(String token) {
		return Character.isJavaIdentifierStart( token.charAt( 0 ) );
	}

	private static String nextNonWhite(String[] tokens, int start) {
        for(int i=start+1;i<tokens.length;i++){
        	if(!ParserHelper.isWhitespace(tokens[i]))
        		return tokens[i];
        }
		return tokens[tokens.length-1];
	}

	private static int getStartingPositionFor(String[] tokens,StringBuilder templateQuery) {
		templateQuery.append(tokens[0]);
		if(!"select".equals(tokens[0].toLowerCase()))
			return 1;
		for(int i=1;i<tokens.length;i++){
			if("from".equals(tokens[i].toLowerCase()))
				templateQuery.append(tokens[i]);
		}
		return tokens.length;
	}
}
