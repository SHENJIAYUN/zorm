package com.zorm.query;

import antlr.SemanticException;
import antlr.collections.AST;

public class SelectExpressionImpl extends FromReferenceNode implements SelectExpression {

	public void resolveIndex(AST parent) throws SemanticException {
		throw new UnsupportedOperationException();
	}

	public void setScalarColumnText(int i) throws SemanticException {
		String text = getFromElement().renderScalarIdentifierSelect( i );
		setText( text );
	}

	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias, AST parent) throws SemanticException {
		return;
	}

	@Override
	public void resolveInFunctionCall(boolean generateJoin, boolean implicitJoin)
			throws SemanticException {
		
	}

	@Override
	public String getDisplayText() {
		return null;
	}

	@Override
	public String getPath() {
		return null;
	}
}
