package com.zorm.query;

import java.sql.PreparedStatement;

import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;

public interface ParameterSpecification {

	public void setExpectedType(Type expectedType);

	public Type getExpectedType();

	public String renderDisplayInfo();

	public int bind(PreparedStatement st, QueryParameters parameters,
			SessionImplementor session, int pos);

}
