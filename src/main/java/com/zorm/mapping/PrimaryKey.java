package com.zorm.mapping;

import java.util.*;
import com.zorm.dialect.Dialect;

public class PrimaryKey extends Constraint{
	
	public String sqlConstraintString(Dialect dialect) {
		StringBuilder buf = new StringBuilder("primary key (");
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName(dialect) );
			if ( iter.hasNext() ) buf.append(", ");
		}
		return buf.append(')').toString();
	}
	
	public String sqlConstraintString(Dialect dialect, String constraintName, String defaultCatalog, String defaultSchema) {
		StringBuilder buf = new StringBuilder(
			dialect.getAddPrimaryKeyConstraintString(constraintName)
		).append('(');
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName(dialect) );
			if ( iter.hasNext() ) buf.append(", ");
		}
		return buf.append(')').toString();
	}
}
