package com.zorm.query;

import java.io.Serializable;

import com.zorm.type.Type;

public class ReturnMetadata implements Serializable{
  private final String[] returnAliases;
  private final Type[] returnTypes;
  
  public ReturnMetadata(String[] returnAliases, Type[] returnTypes) {
		this.returnAliases = returnAliases;
		this.returnTypes = returnTypes;
	}

	public String[] getReturnAliases() {
		return returnAliases;
	}

	public Type[] getReturnTypes() {
		return returnTypes;
	}
}
