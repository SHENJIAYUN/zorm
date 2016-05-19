package com.zorm.config.annotations;

import com.zorm.mapping.Collection;
import com.zorm.mapping.PersistentClass;


public class SetBinder extends CollectionBinder {
	public SetBinder() {
	}

	public SetBinder(boolean sorted) {
		super( sorted );
	}

	@Override
	protected Collection createCollection(PersistentClass persistentClass) {
		return new com.zorm.mapping.Set(getMappings(),persistentClass);
	}
}
