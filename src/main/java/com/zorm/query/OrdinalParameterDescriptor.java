package com.zorm.query;

import com.zorm.type.Type;

public class OrdinalParameterDescriptor {
  private final int ordinalPosition;
  private final Type expectedType;
  private final int sourceLocation;
  
  public OrdinalParameterDescriptor(int ordinalPosition, Type expectedType, int sourceLocation) {
		this.ordinalPosition = ordinalPosition;
		this.expectedType = expectedType;
		this.sourceLocation = sourceLocation;
   }
  
  public int getOrdinalPosition() {
		return ordinalPosition;
	}

	public Type getExpectedType() {
		return expectedType;
	}

	public int getSourceLocation() {
		return sourceLocation;
	}
}
