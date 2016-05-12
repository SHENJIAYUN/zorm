package com.zorm.config.annotations;

import com.zorm.mapping.Collection;
import com.zorm.mapping.PersistentClass;

public class MapBinder extends CollectionBinder {
	public MapBinder(boolean sorted) {
		super( sorted );
	}

	public MapBinder() {
		super();
	}

	@Override
	protected Collection createCollection(PersistentClass persistentClass) {
		return null;
	}
}
