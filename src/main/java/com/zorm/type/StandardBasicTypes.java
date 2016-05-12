package com.zorm.type;

import java.util.HashSet;
import java.util.Set;

import com.zorm.type.descriptor.sql.SqlTypeDescriptor;

public class StandardBasicTypes {
	private static final Set<SqlTypeDescriptor> sqlTypeDescriptors = new HashSet<SqlTypeDescriptor>();

	/**
	 * The standard Hibernate type for mapping {@link Long} to JDBC {@link java.sql.Types#BIGINT BIGINT}.
	 *
	 * @see LongType
	 */
	public static final LongType LONG = LongType.INSTANCE;
	
	public static final IntegerType INTEGER = IntegerType.INSTANCE;
	
	public static final BooleanType BOOLEAN = BooleanType.INSTANCE;
	
	public static final StringType STRING = StringType.INSTANCE;
}
