package com.zorm.jdbc;

import java.sql.ResultSet;

public enum ScrollMode {
	/**
	 * @see java.sql.ResultSet#TYPE_FORWARD_ONLY
	 */
	FORWARD_ONLY( ResultSet.TYPE_FORWARD_ONLY ),

	/**
	 * @see java.sql.ResultSet#TYPE_SCROLL_SENSITIVE
	 */
	SCROLL_SENSITIVE(
			ResultSet.TYPE_SCROLL_SENSITIVE
	),
	/**
	 * Note that since the Hibernate session acts as a cache, you
	 * might need to expicitly evict objects, if you need to see
	 * changes made by other transactions.
	 *
	 * @see java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE
	 */
	SCROLL_INSENSITIVE(
			ResultSet.TYPE_SCROLL_INSENSITIVE
	);
	private final int resultSetType;

	private ScrollMode(int level) {
		this.resultSetType = level;
	}


	/**
	 * @return the JDBC result set type code
	 */
	public int toResultSetType() {
		return resultSetType;
	}


	public boolean lessThan(ScrollMode other) {
		return this.resultSetType < other.resultSetType;
	}

}



