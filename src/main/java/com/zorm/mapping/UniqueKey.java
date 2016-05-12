package com.zorm.mapping;

import java.util.*;

import com.zorm.dialect.Dialect;
import com.zorm.engine.Mapping;

public class UniqueKey extends Constraint{
  public String sqlConstraintString(Dialect dialect){
	  StringBuilder buf = new StringBuilder("unique (");
	  boolean hadNullableColumn = false;
	  Iterator iter = getColumnIterator();
	  while(iter.hasNext()){
		  Column column = (Column) iter.next();
		  if(!hadNullableColumn && column.isNullable()){
			  hadNullableColumn = true;
		  }
		  buf.append(column.getQuotedName(dialect));
		  if(iter.hasNext()){
			  buf.append(", ");
		  }
	  }
	  return !hadNullableColumn || dialect.supportsNotNullUnique()?
			  buf.append(')').toString() : null;
  }
  
    @Override
	public String sqlConstraintString(
			Dialect dialect,
			String constraintName,
			String defaultCatalog, 
			String defaultSchema) {
    	StringBuilder buf = new StringBuilder(dialect.getAddUniqueConstraintString( constraintName )).append('(');
    	Iterator iter = getColumnIterator();
		boolean nullable = false;
		while ( iter.hasNext() ) {
			Column column = (Column) iter.next();
			if ( !nullable && column.isNullable() ) nullable = true;
			buf.append( column.getQuotedName( dialect ) );
			if ( iter.hasNext() ) buf.append( ", " );
		}
		return !nullable || dialect.supportsNotNullUnique() ? buf.append( ')' ).toString() : null;
	}
    
    @Override
    public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) {
		if ( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
			return super.sqlCreateString( dialect, p, defaultCatalog, defaultSchema );
		}
		else {
			return Index.buildSqlCreateIndexString( dialect, getName(), getTable(), getColumnIterator(), true,
					defaultCatalog, defaultSchema );
		}
	}

	@Override
    public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
		if ( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
			return super.sqlDropString( dialect, defaultCatalog, defaultSchema );
		}
		else {
			return Index.buildSqlDropIndexString( dialect, getTable(), getName(), defaultCatalog, defaultSchema );
		}
	}

	@Override
    public boolean isGenerated(Dialect dialect) {
		if ( dialect.supportsNotNullUnique() ) return true;
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			if ( ( (Column) iter.next() ).isNullable() ) {
				return false;
			}
		}
		return true;
	}

}
