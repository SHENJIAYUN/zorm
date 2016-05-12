package com.zorm.util;

import com.zorm.query.SqlTokenTypes;

import antlr.ASTFactory;
import antlr.collections.AST;

public class PathHelper {
	private PathHelper() {
	}

	/**
	 * Turns a path into an AST.
	 *
	 * @param path    The path.
	 * @param factory The AST factory to use.
	 * @return An HQL AST representing the path.
	 */
	public static AST parsePath(String path, ASTFactory factory) {
		String[] identifiers = StringHelper.split( ".", path );
		AST lhs = null;
		for ( int i = 0; i < identifiers.length; i++ ) {
			String identifier = identifiers[i];
			AST child = ASTUtil.create( factory, SqlTokenTypes.IDENT, identifier );
			if ( i == 0 ) {
				lhs = child;
			}
			else {
				lhs = ASTUtil.createBinarySubtree( factory, SqlTokenTypes.DOT, ".", lhs, child );
			}
		}
		return lhs;
	}

	
	public static String getAlias(String path) {
		return StringHelper.root( path );
	}
}
