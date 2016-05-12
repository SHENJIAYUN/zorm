package com.zorm.type.descriptor.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.zorm.type.descriptor.ValueBinder;
import com.zorm.type.descriptor.ValueExtractor;
import com.zorm.type.descriptor.java.JavaTypeDescriptor;
import com.zorm.type.descriptor.WrapperOptions;

public class BigIntTypeDescriptor implements SqlTypeDescriptor{
	public static final BigIntTypeDescriptor INSTANCE = new BigIntTypeDescriptor();
	
	public int getSqlType() {
		return Types.BIGINT;
	}
	
	@Override
	public boolean canBeRemapped() {
		return true;
	}
	
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setLong( index, javaTypeDescriptor.unwrap( value, Long.class, options ) );
			}
		};
	}
	
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getLong( name ), options );
			}
		};
	}
}
