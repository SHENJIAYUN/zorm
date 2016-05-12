package com.zorm.query;

import java.sql.PreparedStatement;

import com.zorm.session.SessionImplementor;

public class PositionalParameterSpecification extends AbstractExplicitParameterSpecification implements ParameterSpecification  {
	private final int hqlPosition;
	
	public PositionalParameterSpecification(int sourceLine, int sourceColumn, int hqlPosition) {
		super( sourceLine, sourceColumn );
		this.hqlPosition = hqlPosition;
	}

	@Override
	public String renderDisplayInfo() {
		return null;
	}

	@Override
	public int bind(PreparedStatement st, QueryParameters parameters,
			SessionImplementor session, int pos) {
		// TODO Auto-generated method stub
		return 0;
	}
}
