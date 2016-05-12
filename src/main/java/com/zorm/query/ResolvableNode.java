package com.zorm.query;

import antlr.SemanticException;
import antlr.collections.AST;

public interface ResolvableNode {
	void resolveInFunctionCall(boolean generateJoin, boolean implicitJoin) throws SemanticException;
	void resolve(boolean generateJoin, boolean implicitJoin) throws SemanticException;
	void resolve(boolean generateJoin, boolean implicitJoin, String classAlias, AST parent) throws SemanticException;
}
