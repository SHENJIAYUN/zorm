package com.zorm.service;

import com.zorm.mapping.Collection;
import com.zorm.mapping.PersistentClass;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.EntityPersister;

public interface PersisterClassResolver extends Service {
	
	Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass metadata);

	Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata);

}