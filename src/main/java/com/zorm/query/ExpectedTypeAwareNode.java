package com.zorm.query;

import com.zorm.type.Type;

public interface ExpectedTypeAwareNode {
	public void setExpectedType(Type expectedType);
	public Type getExpectedType();
}
