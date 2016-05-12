package com.zorm.query;

import java.sql.PreparedStatement;

import com.zorm.session.SessionImplementor;

public class NamedParameterSpecification extends AbstractExplicitParameterSpecification implements ParameterSpecification{
	private final String name;
	
	public NamedParameterSpecification(int sourceLine, int sourceColumn, String name) {
		super( sourceLine, sourceColumn );
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String renderDisplayInfo() {
		return null;
	}

	@Override
	public int bind(PreparedStatement st, QueryParameters parameters,
			SessionImplementor session, int pos) {
		return 0;
	}

}
