package com.zorm.config.annotations;

import com.zorm.mapping.Collection;
import com.zorm.mapping.PersistentClass;


public class BagBinder extends CollectionBinder {
	public BagBinder() {
	}

	@Override
	protected Collection createCollection(PersistentClass persistentClass) {
		return new com.zorm.mapping.Bag( getMappings(), persistentClass );
	}

}
