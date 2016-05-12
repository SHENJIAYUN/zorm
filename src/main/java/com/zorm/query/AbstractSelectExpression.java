package com.zorm.query;

import com.zorm.type.Type;

import antlr.SemanticException;

public abstract class AbstractSelectExpression extends SqlWalkerNode implements SelectExpression {
	
	private String alias;
	private int scalarColumnIndex = -1;
	
	public final void setAlias(String alias) {
		this.alias = alias;
	}
	
	public final String getAlias() {
		return alias;
	}

	public boolean isConstructor() {
		return false;
	}

	public boolean isReturnableEntity() throws SemanticException {
		return false;
	}

	public FromElement getFromElement() {
		return null;
	}

	public boolean isScalar() throws SemanticException {
		// Default implementation:
		// If this node has a data type, and that data type is not an association, then this is scalar.
		Type type = getDataType();
		return type != null && !type.isAssociationType();	// Moved here from SelectClause [jsd]
	}

	public void setScalarColumn(int i) throws SemanticException {
		this.scalarColumnIndex = i;
		setScalarColumnText( i );
	}

	public int getScalarColumnIndex() {
		return scalarColumnIndex;
	}
	
}
