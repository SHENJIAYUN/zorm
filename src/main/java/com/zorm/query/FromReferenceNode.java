package com.zorm.query;

import antlr.SemanticException;

public abstract class FromReferenceNode extends AbstractSelectExpression
     implements ResolvableNode, DisplayableNode, InitializeableNode, PathNode {
	private FromElement fromElement;
	private boolean resolved = false;
	public static final int ROOT_LEVEL = 0;
	
	public void resolve(boolean generateJoin, boolean implicitJoin) throws SemanticException {
		resolve( generateJoin, implicitJoin, null );
	}
	
	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias) throws SemanticException {
		resolve( generateJoin, implicitJoin, classAlias, null );
	}
	
	public boolean isResolved() {
		return resolved;
	}
	
	public void resolveFirstChild() throws SemanticException {
	}
	
	public void setResolved() {
		this.resolved = true;
	}
	
	public void setFromElement(FromElement fromElement) {
		this.fromElement = fromElement;
	}
	
	@Override
    public FromElement getFromElement() {
		return fromElement;
	}

	public void prepareForDot(String propertyName) {
	}
	
	@Override
	public String getPath() {
		return getOriginalText();
	}

	public FromElement getImpliedJoin() {
		return null;
	}
}