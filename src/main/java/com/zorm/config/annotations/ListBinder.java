package com.zorm.config.annotations;

import com.zorm.mapping.Collection;
import com.zorm.mapping.PersistentClass;

public class ListBinder extends CollectionBinder{
	public ListBinder() {
	}

	@Override
	protected Collection createCollection(PersistentClass persistentClass) {
		return null;
	}
}
