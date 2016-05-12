package com.zorm;

public enum FlushMode {
	/**
	 * The {@link Session} is never flushed unless {@link Session#flush}
	 * is explicitly called by the application. This mode is very
	 * efficient for read only transactions.
	 *
	 * @deprecated use {@link #MANUAL} instead.
	 */
	NEVER ( 0 ),

	/**
	 * The {@link Session} is only ever flushed when {@link Session#flush}
	 * is explicitly called by the application. This mode is very
	 * efficient for read only transactions.
	 */
	MANUAL( 0 ),

	/**
	 * The {@link Session} is flushed when {@link Transaction#commit}
	 * is called.
	 */
	COMMIT(5 ),

	/**
	 * The {@link Session} is sometimes flushed before query execution
	 * in order to ensure that queries never return stale state. This
	 * is the default flush mode.
	 */
	AUTO(10 ),

	/**
	 * The {@link Session} is flushed before every query. This is
	 * almost always unnecessary and inefficient.
	 */
	ALWAYS(20 );

	private final int level;

	private FlushMode(int level) {
		this.level = level;
	}
	
	public boolean lessThan(FlushMode other) {
		return this.level<other.level;
	}

	public static boolean isManualFlushMode(FlushMode mode) {
		return MANUAL.level == mode.level;
	}
}
