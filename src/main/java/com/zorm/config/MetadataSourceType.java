package com.zorm.config;

import com.zorm.exception.ZormException;

public enum MetadataSourceType {

	CLASS("class");
	
	private final String name;

	private MetadataSourceType(String name){
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	static MetadataSourceType parsePrecedence(String value){
		if(CLASS.name.equalsIgnoreCase(value)){
			return CLASS;
		}
		else{
			throw new ZormException("Unknown metadata source type value [" + value + "]");
		}
	}
}
