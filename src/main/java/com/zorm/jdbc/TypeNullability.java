package com.zorm.jdbc;

import java.sql.DatabaseMetaData;

public enum TypeNullability {
	NULLABLE,
	NON_NULLABLE,
	UNKNOWN;
	
	public static TypeNullability interpret(short code) {
		switch ( code ) {
			case DatabaseMetaData.typeNullable: {
				return NULLABLE;
			}
			case DatabaseMetaData.typeNoNulls: {
				return NON_NULLABLE;
			}
			case DatabaseMetaData.typeNullableUnknown: {
				return UNKNOWN;
			}
			default: {
				throw new IllegalArgumentException( "Unknown type nullability code [" + code + "] enountered" );
			}
		}
	}
	
}
