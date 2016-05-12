package com.zorm.sql;

import java.util.*;
import com.zorm.LockMode;
import com.zorm.LockOptions;
import com.zorm.dialect.Dialect;

public class SimpleSelect {
	
	private String tableName;
	private String orderBy;
	private Dialect dialect;
	private LockOptions lockOptions = new LockOptions( LockMode.READ);
	private String comment;

	private List columns = new ArrayList();
	private Map aliases = new HashMap();
	private List whereTokens = new ArrayList();
	
	public SimpleSelect(Dialect dialect) {
		this.dialect = dialect;
	}
	
	public SimpleSelect addColumns(String[] columnNames, String[] columnAliases) {
		for ( int i=0; i<columnNames.length; i++ ) {
			if ( columnNames[i]!=null  ) {
				addColumn( columnNames[i], columnAliases[i] );
			}
		}
		return this;
	}

	public SimpleSelect addColumns(String[] columns, String[] aliases, boolean[] ignore) {
		for ( int i=0; i<ignore.length; i++ ) {
			if ( !ignore[i] && columns[i]!=null ) {
				addColumn( columns[i], aliases[i] );
			}
		}
		return this;
	}

	public SimpleSelect addColumns(String[] columnNames) {
		for ( int i=0; i<columnNames.length; i++ ) {
			if ( columnNames[i]!=null ) addColumn( columnNames[i] );
		}
		return this;
	}
	public SimpleSelect addColumn(String columnName) {
		columns.add(columnName);
		//aliases.put( columnName, DEFAULT_ALIAS.toAliasString(columnName) );
		return this;
	}

	public SimpleSelect addColumn(String columnName, String alias) {
		columns.add(columnName);
		aliases.put(columnName, alias);
		return this;
	}

	public SimpleSelect setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public SimpleSelect setLockOptions( LockOptions lockOptions ) {
	   LockOptions.copy(lockOptions, this.lockOptions);
		return this;
	}

	public SimpleSelect setLockMode(LockMode lockMode) {
		this.lockOptions.setLockMode( lockMode );
		return this;
	}

	public SimpleSelect addWhereToken(String token) {
		whereTokens.add(token);
		return this;
	}
	
	private void and() {
		if ( whereTokens.size()>0 ) {
			whereTokens.add("and");
		}
	}

	public SimpleSelect addCondition(String lhs, String op, String rhs) {
		and();
		whereTokens.add( lhs + ' ' + op + ' ' + rhs );
		return this;
	}

	public SimpleSelect addCondition(String lhs, String condition) {
		and();
		whereTokens.add( lhs + ' ' + condition );
		return this;
	}

	public SimpleSelect addCondition(String[] lhs, String op, String[] rhs) {
		for ( int i=0; i<lhs.length; i++ ) {
			addCondition( lhs[i], op, rhs[i] );
		}
		return this;
	}

	public SimpleSelect addCondition(String[] lhs, String condition) {
		for ( int i=0; i<lhs.length; i++ ) {
			if ( lhs[i]!=null ) addCondition( lhs[i], condition );
		}
		return this;
	}

	public String toStatementString() {
		StringBuilder buf = new StringBuilder( 
				columns.size()*10 + 
				tableName.length() + 
				whereTokens.size() * 10 + 
				10 
			);
		
		if ( comment!=null ) {
			buf.append("/* ").append(comment).append(" */ ");
		}
		
		buf.append("select ");
		Set uniqueColumns = new HashSet();
		Iterator iter = columns.iterator();
		boolean appendComma = false;
		while ( iter.hasNext() ) {
			String col = (String) iter.next();
			String alias = (String) aliases.get(col);
			if ( uniqueColumns.add(alias==null ? col : alias) ) {
				if (appendComma) buf.append(", ");
				buf.append(col);
				if ( alias!=null && !alias.equals(col) ) {
					buf.append(" as ")
						.append(alias);
				}
				appendComma = true;
			}
		}
		
		buf.append(" from ")
			.append( dialect.appendLockHint(lockOptions, tableName) );
		
		if ( whereTokens.size() > 0 ) {
			buf.append(" where ")
				.append( toWhereClause() );
		}
		
		if (orderBy!=null) buf.append(orderBy);
		
		if (lockOptions!=null) {
			buf.append( dialect.getForUpdateString(lockOptions) );
		}

		return dialect.transformSelectString( buf.toString() );
	}

	public String toWhereClause() {
		StringBuilder buf = new StringBuilder( whereTokens.size() * 5 );
		Iterator iter = whereTokens.iterator();
		while ( iter.hasNext() ) {
			buf.append( iter.next() );
			if ( iter.hasNext() ) buf.append(' ');
		}
		return buf.toString();
	}

	public SimpleSelect setOrderBy(String orderBy) {
		this.orderBy = orderBy;
		return this;
	}

	public SimpleSelect setComment(String comment) {
		this.comment = comment;
		return this;
	}

}
