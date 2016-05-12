package com.zorm.type;

import com.zorm.dialect.Dialect;
import com.zorm.type.descriptor.java.StringTypeDescriptor;
import com.zorm.type.descriptor.sql.VarcharTypeDescriptor;

public class StringType extends AbstractSingleColumnStandardBasicType<String>
implements DiscriminatorType<String>{
	public static final StringType INSTANCE = new StringType();
	
	public StringType() {
		super( VarcharTypeDescriptor.INSTANCE, StringTypeDescriptor.INSTANCE );
	}
	
	public String getName() {
		return "string";
	}
	
	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
	
	public String objectToSQLString(String value, Dialect dialect) throws Exception {
		return '\'' + value + '\'';
	}
	
	public String stringToObject(String xml) throws Exception {
		return xml;
	}
	
	public String toString(String value) {
		return value;
	}
}
