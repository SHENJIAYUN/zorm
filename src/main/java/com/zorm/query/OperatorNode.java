package com.zorm.query;

import com.zorm.type.Type;

import antlr.SemanticException;

public interface OperatorNode {
	public abstract void initialize() throws SemanticException;
	public Type getDataType();
}
