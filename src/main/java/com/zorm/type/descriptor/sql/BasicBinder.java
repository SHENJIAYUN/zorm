package com.zorm.type.descriptor.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.type.descriptor.ValueBinder;
import com.zorm.type.descriptor.WrapperOptions;
import com.zorm.type.descriptor.java.JavaTypeDescriptor;

public abstract class BasicBinder<J> implements ValueBinder<J> {

	private static final Log log = LogFactory.getLog(BasicBinder.class);
	private static final String BIND_MSG_TEMPLATE = "binding parameter [%s] as [%s] - %s";
    private static final String NULL_BIND_MSG_TEMPLATE = "binding parameter [%s] as [%s] - <null>";

	private final JavaTypeDescriptor<J> javaDescriptor;
	private final SqlTypeDescriptor sqlDescriptor;

	public JavaTypeDescriptor<J> getJavaDescriptor() {
		return javaDescriptor;
	}

	public SqlTypeDescriptor getSqlDescriptor() {
		return sqlDescriptor;
	}

	public BasicBinder(JavaTypeDescriptor<J> javaDescriptor, SqlTypeDescriptor sqlDescriptor) {
		this.javaDescriptor = javaDescriptor;
		this.sqlDescriptor = sqlDescriptor;
	}

	/**
	 * {@inheritDoc}
	 */
	public final void bind(PreparedStatement st, J value, int index, WrapperOptions options) throws SQLException {
        if ( value == null ) {
            st.setNull( index, sqlDescriptor.getSqlType() );
        }
        else {
            doBind( st, value, index, options );
        }
	}

	/**
	 * Perform the binding.  Safe to assume that value is not null.
	 *
	 * @param st The prepared statement
	 * @param value The value to bind (not null).
	 * @param index The index at which to bind
	 * @param options The binding options
	 *
	 * @throws SQLException Indicates a problem binding to the prepared statement.
	 */
	protected abstract void doBind(PreparedStatement st, J value, int index, WrapperOptions options) throws SQLException;
}
