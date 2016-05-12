package com.zorm.persister;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.zorm.config.Configuration;
import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.mapping.Collection;
import com.zorm.mapping.PersistentClass;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.service.PersisterClassResolver;
import com.zorm.service.ServiceRegistryAwareService;
import com.zorm.service.ServiceRegistryImplementor;
import com.zorm.session.SessionFactoryImplementor;

public class PersisterFactoryImpl implements PersisterFactory, ServiceRegistryAwareService{

	public static final Class[] ENTITY_PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
		PersistentClass.class,
		SessionFactoryImplementor.class,
		Mapping.class
    };
	
	private static final Class[] COLLECTION_PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
		Collection.class,
		Configuration.class,
		SessionFactoryImplementor.class
    };
	
	private ServiceRegistryImplementor serviceRegistry;
	
	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public CollectionPersister createCollectionPersister(
			Collection collectionMetadata,
			Configuration cfg, 
			SessionFactoryImplementor factory)
			throws ZormException {
		Class<? extends CollectionPersister> persisterClass = collectionMetadata.getCollectionPersisterClass();
		if ( persisterClass == null ) {
			persisterClass = serviceRegistry.getService( PersisterClassResolver.class ).getCollectionPersisterClass( collectionMetadata );
		}

		return create( persisterClass, COLLECTION_PERSISTER_CONSTRUCTOR_ARGS,cfg, collectionMetadata, factory );
	}
	
	private static CollectionPersister create(
			Class<? extends CollectionPersister> persisterClass,
			Class[] persisterConstructorArgs,
			Object cfg,
			Object collectionMetadata,
			SessionFactoryImplementor factory) throws ZormException {
		try {
			Constructor<? extends CollectionPersister> constructor = persisterClass.getConstructor( persisterConstructorArgs );
			try {
				return constructor.newInstance( collectionMetadata, cfg, factory );
			}
			catch (MappingException e) {
				throw e;
			}
			catch (InvocationTargetException e) {
				Throwable target = e.getTargetException();
				if ( target instanceof ZormException ) {
					throw (ZormException) target;
				}
				else {
					throw new MappingException( "Could not instantiate collection persister " + persisterClass.getName(), target );
				}
			}
			catch (Exception e) {
				throw new MappingException( "Could not instantiate collection persister " + persisterClass.getName(), e );
			}
		}
		catch (MappingException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MappingException( "Could not get constructor for " + persisterClass.getName(), e );
		}
	}

	@Override
	public EntityPersister createEntityPersister(
			PersistentClass metadata,
			SessionFactoryImplementor factory, 
			Mapping cfg)
			throws ZormException {
		Class<? extends EntityPersister> persisterClass = metadata.getEntityPersisterClass();
		if ( persisterClass == null ) {
			persisterClass = serviceRegistry.getService( PersisterClassResolver.class ).getEntityPersisterClass( metadata );
		}
		return create( persisterClass, ENTITY_PERSISTER_CONSTRUCTOR_ARGS, metadata, factory, cfg );
	}

	private EntityPersister create(
			Class<? extends EntityPersister> persisterClass,
			Class[] entityPersisterConstructorArgs,
			PersistentClass metadata,
			SessionFactoryImplementor factory, 
			Mapping cfg) {
		try {
			Constructor<? extends EntityPersister> constructor = persisterClass.getConstructor( entityPersisterConstructorArgs );
			try {
				return constructor.newInstance( metadata, factory, cfg );
			}
			catch (MappingException e) {
				throw e;
			}
			catch (InvocationTargetException e) {
				Throwable target = e.getTargetException();
				if ( target instanceof ZormException ) {
					throw (ZormException) target;
				}
				else {
					throw new MappingException( "Could not instantiate persister " + persisterClass.getName(), target );
				}
			}
			catch (Exception e) {
				throw new MappingException( "Could not instantiate persister " + persisterClass.getName(), e );
			}
		}
		catch (MappingException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MappingException( "Could not get constructor for " + persisterClass.getName(), e );
		}
	}
	
	

}
