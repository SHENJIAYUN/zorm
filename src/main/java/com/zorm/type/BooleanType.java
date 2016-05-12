package com.zorm.type;

import java.io.Serializable;

import com.zorm.dialect.Dialect;
import com.zorm.type.descriptor.java.BooleanTypeDescriptor;
import com.zorm.type.descriptor.sql.SqlTypeDescriptor;

public class BooleanType extends AbstractSingleColumnStandardBasicType<Boolean>
		implements PrimitiveType<Boolean>, DiscriminatorType<Boolean> {
	public static final BooleanType INSTANCE = new BooleanType();

	public BooleanType() {
		this(com.zorm.type.descriptor.sql.BooleanTypeDescriptor.INSTANCE,
				BooleanTypeDescriptor.INSTANCE);
	}

	protected BooleanType(SqlTypeDescriptor sqlTypeDescriptor,
			BooleanTypeDescriptor javaTypeDescriptor) {
		super(sqlTypeDescriptor, javaTypeDescriptor);
	}

	public String getName() {
		return "boolean";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), boolean.class.getName(),
				Boolean.class.getName() };
	}

	public Class getPrimitiveClass() {
		return boolean.class;
	}

	public Serializable getDefaultValue() {
		return Boolean.FALSE;
	}

	public Boolean stringToObject(String string) {
		return fromString(string);
	}

	@SuppressWarnings({ "UnnecessaryUnboxing" })
	public String objectToSQLString(Boolean value, Dialect dialect) {
		return dialect.toBooleanValueString(value.booleanValue());
	}
}





