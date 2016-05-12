package com.zorm.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.zorm.dialect.Dialect;
import com.zorm.engine.Mapping;
import com.zorm.exception.ZormException;
import com.zorm.util.StringHelper;

public class Index implements RelationalModel, Serializable{
	private Table table;
	private List columns = new ArrayList();
	private String name;

	public String sqlCreateString(Dialect dialect, Mapping mapping, String defaultCatalog, String defaultSchema)
			throws ZormException {
		return buildSqlCreateIndexString(
				dialect,
				getName(),
				getTable(),
				getColumnIterator(),
				false,
				defaultCatalog,
				defaultSchema
		);
	}

	public static String buildSqlDropIndexString(
			Dialect dialect,
			Table table,
			String name,
			String defaultCatalog,
			String defaultSchema
	) {
		return "drop index " +
				StringHelper.qualify(
						table.getQualifiedName( dialect, defaultCatalog, defaultSchema ),
						name
				);
	}

	public static String buildSqlCreateIndexString(
			Dialect dialect,
			String name,
			Table table,
			Iterator columns,
			boolean unique,
			String defaultCatalog,
			String defaultSchema
	) {
		StringBuilder buf = new StringBuilder( "create" )
				.append( unique ?
						" unique" :
						"" )
				.append( " index " )
				.append( dialect.qualifyIndexName() ?
						name :
						StringHelper.unqualify( name ) )
				.append( " on " )
				.append( table.getQualifiedName( dialect, defaultCatalog, defaultSchema ) )
				.append( " (" );
		Iterator iter = columns;
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName( dialect ) );
			if ( iter.hasNext() ) buf.append( ", " );
		}
		buf.append( ")" );
		return buf.toString();
	}


	// Used only in Table for sqlCreateString (but commented out at the moment)
	public String sqlConstraintString(Dialect dialect) {
		StringBuilder buf = new StringBuilder( " index (" );
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName( dialect ) );
			if ( iter.hasNext() ) buf.append( ", " );
		}
		return buf.append( ')' ).toString();
	}

	public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
		return "drop index " +
				StringHelper.qualify(
						table.getQualifiedName( dialect, defaultCatalog, defaultSchema ),
						name
				);
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public int getColumnSpan() {
		return columns.size();
	}

	public Iterator getColumnIterator() {
		return columns.iterator();
	}

	public void addColumn(Column column) {
		if ( !columns.contains( column ) ) columns.add( column );
	}

	public void addColumns(Iterator extraColumns) {
		while ( extraColumns.hasNext() ) addColumn( (Column) extraColumns.next() );
	}

	/**
	 * @param column
	 * @return true if this constraint already contains a column with same name.
	 */
	public boolean containsColumn(Column column) {
		return columns.contains( column );
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return getClass().getName() + "(" + getName() + ")";
	}
}
