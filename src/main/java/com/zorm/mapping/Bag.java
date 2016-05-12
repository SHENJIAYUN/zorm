package com.zorm.mapping;

import com.zorm.config.Mappings;
import com.zorm.exception.MappingException;
import com.zorm.type.CollectionType;

public class Bag extends Collection {

	public Bag(Mappings mappings, PersistentClass owner) {
		super( mappings, owner );
	}

	@Override
	public CollectionType getDefaultCollectionType() throws MappingException {
		return getMappings().getTypeResolver()
				.getTypeFactory()
				.bag( getRole(), getReferencedPropertyName() );
	}

	@Override
	void createPrimaryKey() {

	}

}
