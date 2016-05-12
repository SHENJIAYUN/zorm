package com.zorm.mapping;

import java.io.Serializable;
import java.util.ArrayList;

public class Join implements Serializable{

	private Table table;
	private ArrayList properties = new ArrayList();
	private KeyValue key;
	
	public Table getTable() {
		return table;
	}

	public boolean containsProperty(Property prop) {
		return properties.contains(prop);
	}

	public KeyValue getKey() {
		return key;
	}
	
	public void setKey(KeyValue key) {
		this.key = key;
	}

}
