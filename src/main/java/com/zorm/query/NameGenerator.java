package com.zorm.query;

import com.zorm.exception.MappingException;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.type.Type;

public final class NameGenerator {
	/**
	 * Private empty constructor (checkstyle says utility classes should not have default constructors).
	 */
	private NameGenerator() {
	}

	public static String[][] generateColumnNames(Type[] types, SessionFactoryImplementor f) throws MappingException {
		String[][] columnNames = new String[types.length][];
		for ( int i = 0; i < types.length; i++ ) {
			int span = types[i].getColumnSpan( f );
			columnNames[i] = new String[span];
			for ( int j = 0; j < span; j++ ) {
				columnNames[i][j] = NameGenerator.scalarName( i, j );
			}
		}
		return columnNames;
	}

	public static String scalarName(int x, int y) {
		return new StringBuilder()
				.append( "col_" )
				.append( x )
				.append( '_' )
				.append( y )
				.append( '_' )
				.toString();
	}
}
