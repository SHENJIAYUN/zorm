package com.zorm.type;

import java.io.Serializable;
import java.util.Comparator;

import com.zorm.dialect.Dialect;
import com.zorm.session.SessionImplementor;
import com.zorm.type.descriptor.java.LongTypeDescriptor;
import com.zorm.type.descriptor.sql.BigIntTypeDescriptor;


public class LongType extends AbstractSingleColumnStandardBasicType<Long>
         implements PrimitiveType<Long>, DiscriminatorType<Long>, VersionType<Long>{

	public static final LongType INSTANCE = new LongType();
	private static final Long ZERO = Long.valueOf( 0 );

	public LongType() {
		super( BigIntTypeDescriptor.INSTANCE, LongTypeDescriptor.INSTANCE );
	}
	
	public String getName() {
		return "long";
	}
	
	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), long.class.getName(), Long.class.getName() };
	}
	
	public Serializable getDefaultValue() {
		return ZERO;
	}
	
	public Class getPrimitiveClass() {
		return long.class;
	}
	
	public Long stringToObject(String xml) throws Exception {
		return Long.valueOf( xml );
	}
	
	public Long next(Long current, SessionImplementor session) {
		return current + 1l;
	}
	
	public Long seed(SessionImplementor session) {
		return ZERO;
	}
	
	public Comparator<Long> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}
	
	public String objectToSQLString(Long value, Dialect dialect) throws Exception {
		return value.toString();
	}
	
}
