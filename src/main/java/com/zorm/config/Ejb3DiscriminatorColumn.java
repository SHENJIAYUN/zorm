package com.zorm.config;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;

import com.zorm.exception.AssertionFailure;

public class Ejb3DiscriminatorColumn extends Ejb3Column {
	// 标识列默认列名
	private static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "DTYPE";
	private static final String DEFAULT_DISCRIMINATOR_TYPE = "string";
	private static final int DEFAULT_DISCRIMINATOR_LENGTH = 31;

	private String discriminatorTypeName;

	public Ejb3DiscriminatorColumn() {
		super();
		setLogicalColumnName(DEFAULT_DISCRIMINATOR_COLUMN_NAME);
		setNullable(false);
		setDiscriminatorTypeName(DEFAULT_DISCRIMINATOR_TYPE);
		setLength(DEFAULT_DISCRIMINATOR_LENGTH);
	}

	public void setDiscriminatorTypeName(String discriminatorTypeName) {
		this.discriminatorTypeName = discriminatorTypeName;
	}

	public static Ejb3DiscriminatorColumn buildDiscriminatorColumn(
			DiscriminatorType type, DiscriminatorColumn discAnn,
			Mappings mappings) {

		Ejb3DiscriminatorColumn discriminatorColumn = new Ejb3DiscriminatorColumn();
		discriminatorColumn.setMappings(mappings);
		discriminatorColumn.setImplicit(true);
		if (discAnn != null) {
			discriminatorColumn.setImplicit(false);
			if (!BinderHelper
					.isEmptyAnnotationValue(discAnn.columnDefinition())) {
				discriminatorColumn.setSqlType(discAnn.columnDefinition());
			}
			if (!BinderHelper.isEmptyAnnotationValue(discAnn.name())) {
				discriminatorColumn.setLogicalColumnName(discAnn.name());
			}
			discriminatorColumn.setNullable(false);
		}
		if (DiscriminatorType.CHAR.equals(type)) {
			discriminatorColumn.setDiscriminatorTypeName("character");
			discriminatorColumn.setImplicit(false);
		} else if (DiscriminatorType.INTEGER.equals(type)) {
			discriminatorColumn.setDiscriminatorTypeName("integer");
			discriminatorColumn.setImplicit(false);
		} else if (DiscriminatorType.STRING.equals(type)) {
			if (discAnn != null)
				discriminatorColumn.setLength(discAnn.length());
			discriminatorColumn.setDiscriminatorTypeName("string");
		} else {
			throw new AssertionFailure("Unknown discriminator type: " + type);
		}
		discriminatorColumn.bind();
		return discriminatorColumn;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Ejb3DiscriminatorColumn");
		sb.append("{logicalColumnName'").append(getLogicalColumnName())
				.append('\'');
		sb.append(", discriminatorTypeName='").append(discriminatorTypeName)
				.append('\'');
		sb.append('}');
		return sb.toString();
	}

	public String getDiscriminatorTypeName() {
		return discriminatorTypeName;
	}

}
