package com.zorm.dialect.function;

import java.util.HashMap;
import java.util.Map;

import com.zorm.dialect.Dialect;

public class SQLFunctionRegistry {
	private final Dialect dialect;
	private final Map<String, SQLFunction> userFunctions;
	
	public SQLFunctionRegistry(Dialect dialect, Map<String, SQLFunction> userFunctions) {
		this.dialect = dialect;
		this.userFunctions = new HashMap<String, SQLFunction>();
		this.userFunctions.putAll( userFunctions );
	}
	
	public SQLFunction findSQLFunction(String functionName) {
		String name = functionName.toLowerCase();
		SQLFunction userFunction = userFunctions.get( name );
		return userFunction != null
				? userFunction
				: (SQLFunction) dialect.getFunctions().get( name );
	}

	public boolean hasFunction(String functionName) {
		String name = functionName.toLowerCase();
		return userFunctions.containsKey( name ) || dialect.getFunctions().containsKey( name );
	}
}
