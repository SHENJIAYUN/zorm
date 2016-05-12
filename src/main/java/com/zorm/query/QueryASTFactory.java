package com.zorm.query;

import antlr.ASTFactory;

public class QueryASTFactory extends ASTFactory {
	public Class getASTNodeType(int tokenType) {
		return Node.class;
	}
}
