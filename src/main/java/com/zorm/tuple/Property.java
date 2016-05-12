package com.zorm.tuple;

import java.io.Serializable;

import com.zorm.type.Type;

public abstract class Property implements Serializable{
	private String name;
	private String node;
	private Type type;

	/**
	 * Constructor for Property instances.
	 *
	 * @param name The name by which the property can be referenced within
	 * its owner.
	 * @param node The node name to use for XML-based representation of this
	 * property.
	 * @param type The Hibernate Type of this property.
	 */
	protected Property(String name, String node, Type type) {
		this.name = name;
		this.node = node;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public String getNode() {
		return node;
	}

	public Type getType() {
		return type;
	}
	
	public String toString() {
		return "Property(" + name + ':' + type.getName() + ')';
	}
}
