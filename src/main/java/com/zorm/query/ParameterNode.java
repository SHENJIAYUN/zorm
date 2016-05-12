package com.zorm.query;

import com.zorm.session.SessionFactoryImplementor;
import com.zorm.type.Type;

public class ParameterNode extends SqlWalkerNode implements DisplayableNode, ExpectedTypeAwareNode {
	private ParameterSpecification parameterSpecification;

	public ParameterSpecification getHqlParameterSpecification() {
		return parameterSpecification;
	}

	public void setHqlParameterSpecification(ParameterSpecification parameterSpecification) {
		this.parameterSpecification = parameterSpecification;
	}

	public String getDisplayText() {
		return "{" + ( parameterSpecification == null ? "???" : parameterSpecification.renderDisplayInfo() ) + "}";
	}

	public void setExpectedType(Type expectedType) {
		getHqlParameterSpecification().setExpectedType( expectedType );
		setDataType( expectedType );
	}

	public Type getExpectedType() {
		return getHqlParameterSpecification() == null ? null : getHqlParameterSpecification().getExpectedType();
	}

	public String getRenderText(SessionFactoryImplementor sessionFactory) {
		int count = 0;
		if ( getExpectedType() != null && ( count = getExpectedType().getColumnSpan( sessionFactory ) ) > 1 ) {
			StringBuilder buffer = new StringBuilder();
			buffer.append( "(?" );
			for ( int i = 1; i < count; i++ ) {
				buffer.append( ", ?" );
			}
			buffer.append( ")" );
			return buffer.toString();
		}
		else {
			return "?";
		}
	}
}
