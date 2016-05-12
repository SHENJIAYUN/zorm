package com.zorm.query;

import antlr.SemanticException;

public interface SelectExpression {
	void setScalarColumnText(int i) throws SemanticException;
	
	boolean isScalar() throws SemanticException;

	FromElement getFromElement();

	void setText(String text);
}
