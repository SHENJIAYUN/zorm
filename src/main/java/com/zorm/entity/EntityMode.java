package com.zorm.entity;

public enum EntityMode {
	POJO( "pojo" ),
	MAP( "dynamic-map" );

	private final String name;

	EntityMode(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	private static final String DYNAMIC_MAP_NAME = MAP.name.toUpperCase();

	/**
	 * Legacy-style entity-mode name parsing.  <b>Case insensitive</b>
	 *
	 * @param entityMode The entity mode name to evaluate
	 *
	 * @return The appropriate entity mode; {@code null} for incoming {@code entityMode} param is treated by returning
	 * {@link #POJO}.
	 */
	public static EntityMode parse(String entityMode) {
		if ( entityMode == null ) {
			return POJO;
		}
		entityMode = entityMode.toUpperCase();
		if ( DYNAMIC_MAP_NAME.equals( entityMode ) ) {
			return MAP;
		}
		return valueOf( entityMode );
	}
}
