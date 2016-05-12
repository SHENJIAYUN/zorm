package com.zorm.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ResultSetMappingDefinition implements Serializable{
  private final String name;
  private final List<NativeSQLQueryReturn> queryReturns = new ArrayList<NativeSQLQueryReturn>();

	public ResultSetMappingDefinition(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void addQueryReturn(NativeSQLQueryReturn queryReturn) {
		queryReturns.add( queryReturn );
	}
	
	public NativeSQLQueryReturn[] getQueryReturns() {
		return queryReturns.toArray( new NativeSQLQueryReturn[queryReturns.size()] );
	}
}
