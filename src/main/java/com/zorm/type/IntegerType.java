package com.zorm.type;

import java.io.Serializable;
import java.util.Comparator;

import com.zorm.dialect.Dialect;
import com.zorm.session.SessionImplementor;
import com.zorm.type.descriptor.java.IntegerTypeDescriptor;


public class IntegerType extends AbstractSingleColumnStandardBasicType<Integer>
       implements PrimitiveType<Integer>,DiscriminatorType<Integer>,VersionType<Integer>
{
	public static final IntegerType INSTANCE = new IntegerType();
	public static final Integer ZERO = Integer.valueOf( 0 );
	
	public IntegerType() {
		super( com.zorm.type.descriptor.sql.IntegerTypeDescriptor.INSTANCE, IntegerTypeDescriptor.INSTANCE );
	}
	
	public String getName() {
		return "integer";
	}
	
	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), int.class.getName(), Integer.class.getName() };
	}
	
	public Serializable getDefaultValue() {
		return ZERO;
	}
	
	public Class getPrimitiveClass() {
		return int.class;
	}
	
	public String objectToSQLString(Integer value, Dialect dialect) throws Exception {
		return toString( value );
	}
	
	public Integer seed(SessionImplementor session) {
		return ZERO;
	}
	
	public Integer next(Integer current, SessionImplementor session) {
		return Integer.valueOf( current.intValue() + 1 );
	}
	
	public Comparator<Integer> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}

	@Override
	public Integer stringToObject(String xml) throws Exception {
		return fromString( xml );
	}
}
