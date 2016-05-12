package com.zorm.config;

import com.zorm.annotations.reflection.XClass;
import com.zorm.annotations.reflection.XProperty;
import com.zorm.exception.MappingException;
import com.zorm.util.StringHelper;

public class WrappedInferredData implements PropertyData {
	private PropertyData wrappedInferredData;
	private String propertyName;

	public XClass getClassOrElement() throws MappingException {
		return wrappedInferredData.getClassOrElement();
	}

	public String getClassOrElementName() throws MappingException {
		return wrappedInferredData.getClassOrElementName();
	}

	public AccessType getDefaultAccess() {
		return wrappedInferredData.getDefaultAccess();
	}

	public XProperty getProperty() {
		return wrappedInferredData.getProperty();
	}

	public XClass getDeclaringClass() {
		return wrappedInferredData.getDeclaringClass();
	}

	public XClass getPropertyClass() throws MappingException {
		return wrappedInferredData.getPropertyClass();
	}

	public String getPropertyName() throws MappingException {
		return propertyName;
	}

	public String getTypeName() throws MappingException {
		return wrappedInferredData.getTypeName();
	}

	public WrappedInferredData(PropertyData inferredData, String suffix) {
		this.wrappedInferredData = inferredData;
		this.propertyName = StringHelper.qualify( inferredData.getPropertyName(), suffix );
	}
}
