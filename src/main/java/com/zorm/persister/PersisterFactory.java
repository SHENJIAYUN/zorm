package com.zorm.persister;

import com.zorm.config.Configuration;
import com.zorm.engine.Mapping;
import com.zorm.exception.ZormException;
import com.zorm.mapping.Collection;
import com.zorm.mapping.PersistentClass;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.service.Service;
import com.zorm.session.SessionFactoryImplementor;

public interface PersisterFactory extends Service{

	EntityPersister createEntityPersister(
			PersistentClass model, 
            SessionFactoryImplementor sessionFactoryImpl,
			Mapping cgd) throws ZormException;

	CollectionPersister createCollectionPersister(
			Collection model,
			Configuration cfg, 
			SessionFactoryImplementor factory) throws ZormException;

}
