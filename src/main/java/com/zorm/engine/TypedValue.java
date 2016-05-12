package com.zorm.engine;

import java.io.Serializable;

import com.zorm.entity.EntityMode;
import com.zorm.type.Type;

public final class TypedValue implements Serializable {
	private final Type type;
	private final Object value;
	private final EntityMode entityMode;

	public TypedValue(Type type, Object value) {
		this( type, value, EntityMode.POJO );
	}

	public TypedValue(Type type, Object value, EntityMode entityMode) {
		this.type = type;
		this.value=value;
		this.entityMode = entityMode;
	}

	public Object getValue() {
		return value;
	}

	public Type getType() {
		return type;
	}

	public String toString() {
		return value==null ? "null" : value.toString();
	}

	public int hashCode() {
		//int result = 17;
		//result = 37 * result + type.hashCode();
		//result = 37 * result + ( value==null ? 0 : value.hashCode() );
		//return result;
		return value==null ? 0 : type.getHashCode(value );
	}

	public boolean equals(Object other) {
		if ( !(other instanceof TypedValue) ) return false;
		TypedValue that = (TypedValue) other;
		/*return that.type.equals(type) && 
			EqualsHelper.equals(that.value, value);*/
		return type.getReturnedClass() == that.type.getReturnedClass() &&
			type.isEqual(that.value, value );
	}

}

