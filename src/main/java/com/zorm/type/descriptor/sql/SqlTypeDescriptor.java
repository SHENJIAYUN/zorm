package com.zorm.type.descriptor.sql;

import java.io.Serializable;

import com.zorm.type.descriptor.ValueBinder;
import com.zorm.type.descriptor.ValueExtractor;
import com.zorm.type.descriptor.java.JavaTypeDescriptor;

public interface SqlTypeDescriptor extends Serializable{
	/**
	 * Return the {@linkplain java.sql.Types JDBC type-code} for the column mapped by this type.
	 *
	 * @return typeCode The JDBC type-code
	 */
	public int getSqlType();
	
	/**
	 * Is this descriptor available for remapping?
	 *
	 * @return {@code true} indicates this descriptor can be remapped; otherwise, {@code false}
	 *
	 * @see org.hibernate.type.descriptor.WrapperOptions#remapSqlTypeDescriptor
	 * @see org.hibernate.dialect.Dialect#remapSqlTypeDescriptor
	 */
	public boolean canBeRemapped();
	
	public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor);

	public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor);
}
