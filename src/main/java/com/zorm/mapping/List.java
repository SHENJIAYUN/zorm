package com.zorm.mapping;

import com.zorm.config.Mappings;
import com.zorm.exception.MappingException;
import com.zorm.type.CollectionType;

public class List extends IndexedCollection{
	private int baseIndex;

	public boolean isList() {
		return true;
	}
	
	public List(Mappings mappings, PersistentClass owner) {
		super( mappings, owner );
	}
	
	public int getBaseIndex() {
		return baseIndex;
	}
	
	public void setBaseIndex(int baseIndex) {
		this.baseIndex = baseIndex;
	}

	@Override
	public CollectionType getDefaultCollectionType() throws MappingException {
		return getMappings().getTypeResolver()
				.getTypeFactory()
				.list( getRole(), getReferencedPropertyName() );
	}
}
