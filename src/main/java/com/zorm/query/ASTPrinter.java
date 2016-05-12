package com.zorm.query;

import java.util.*;

import com.zorm.util.ASTUtil;

public class ASTPrinter {
	private final Map tokenTypeNameCache;
	private final boolean showClassNames;
	
	public ASTPrinter(Class tokenTypeConstants) {
		this( ASTUtil.generateTokenNameCache( tokenTypeConstants ), true );
	}
	
	private ASTPrinter(Map tokenTypeNameCache, boolean showClassNames) {
		this.tokenTypeNameCache = tokenTypeNameCache;
		this.showClassNames = showClassNames;
	}
}
