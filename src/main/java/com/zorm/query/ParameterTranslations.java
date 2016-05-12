package com.zorm.query;

import com.zorm.type.Type;

public interface ParameterTranslations {

	public int getOrdinalParameterCount();

	public boolean supportsOrdinalParameterMetadata();

	public Type getNamedParameterExpectedType(String name);

	public Type getOrdinalParameterExpectedType(int i);

}
