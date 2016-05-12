package com.zorm.engine;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.zorm.collection.PersistentCollection;
import com.zorm.entity.EntityKey;

public class BatchFetchQueue {
	private final PersistenceContext context;
	
	private final Map<EntityKey, SubselectFetch> subselectsByEntityKey = new HashMap<EntityKey, SubselectFetch>(8);
	private final Map<String, LinkedHashMap<CollectionEntry, PersistentCollection>> batchLoadableCollections =
			new HashMap<String, LinkedHashMap <CollectionEntry, PersistentCollection>>(8);
	
	public BatchFetchQueue(PersistenceContext context) {
		this.context = context;
	}

	public void clearSubselects() {
		subselectsByEntityKey.clear();
	}

	public void removeBatchLoadableCollection(CollectionEntry ce) {
		LinkedHashMap<CollectionEntry, PersistentCollection> map =  batchLoadableCollections.get( ce.getLoadedPersister().getRole() );
		if ( map != null ) {
			map.remove( ce );
		}
	}
}
