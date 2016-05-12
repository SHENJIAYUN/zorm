package com.zorm;

public enum ConnectionReleaseMode {
	
	AFTER_STATEMENT("after_statement"),

	AFTER_TRANSACTION("after_transaction"),

	ON_CLOSE("on_close");

	private final String name;
	
	ConnectionReleaseMode(String name){
		this.name = name;
	}
	
	public static ConnectionReleaseMode parse(String name){
		return ConnectionReleaseMode.valueOf( name.toUpperCase() );
	}
	
}
