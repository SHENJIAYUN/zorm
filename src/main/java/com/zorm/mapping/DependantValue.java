package com.zorm.mapping;

import com.zorm.config.Mappings;
import com.zorm.exception.MappingException;
import com.zorm.type.Type;

public class DependantValue extends SimpleValue {
	private KeyValue wrappedValue;
	private boolean nullable;
	private boolean updateable;

	public DependantValue(Mappings mappings, Table table, KeyValue prototype) {
		super( mappings, table );
		this.wrappedValue = prototype;
	}

	public Type getType() throws MappingException {
		return wrappedValue.getType();
	}

	public void setTypeUsingReflection(String className, String propertyName) {}
	
//	public Object accept(ValueVisitor visitor) {
//		return visitor.accept(this);
//	}

	public boolean isNullable() {
		return nullable;
	
	}
	
	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}
	
	public boolean isUpdateable() {
		return updateable;
	}
	
	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}
}
