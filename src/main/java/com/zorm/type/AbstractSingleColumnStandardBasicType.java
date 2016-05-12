package com.zorm.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.zorm.exception.ZormException;
import com.zorm.session.SessionImplementor;
import com.zorm.type.descriptor.java.JavaTypeDescriptor;
import com.zorm.type.descriptor.sql.SqlTypeDescriptor;

public abstract class AbstractSingleColumnStandardBasicType<T> extends AbstractStandardBasicType<T>
    implements SingleColumnType<T>{
	
	public AbstractSingleColumnStandardBasicType(SqlTypeDescriptor sqlTypeDescriptor, JavaTypeDescriptor<T> javaTypeDescriptor) {
		super( sqlTypeDescriptor, javaTypeDescriptor );
	}
	
	public final int sqlType() {
		return getSqlTypeDescriptor().getSqlType();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public final void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session)
			throws ZormException, SQLException {
		if ( settable[0] ) {
			nullSafeSet( st, value, index, session );
		}
	}
}
