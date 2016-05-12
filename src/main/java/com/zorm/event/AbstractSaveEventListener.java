package com.zorm.event;

import java.io.Serializable;
import java.util.Map;

import com.zorm.LockMode;
import com.zorm.action.AbstractEntityInsertAction;
import com.zorm.action.CascadingAction;
import com.zorm.action.EntityInsertAction;
import com.zorm.engine.Cascade;
import com.zorm.engine.ForeignKeys;
import com.zorm.engine.Status;
import com.zorm.engine.Versioning;
import com.zorm.entity.EntityEntry;
import com.zorm.entity.EntityKey;
import com.zorm.exception.IdentifierGenerationException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;
import com.zorm.util.IdentifierGeneratorHelper;
import com.zorm.util.TypeHelper;

public abstract class AbstractSaveEventListener extends AbstractReassociateEventListener{
	
	public enum EntityState{
	        PERSISTENT, TRANSIENT, DETACHED, DELETED
	  }
	
	protected abstract CascadingAction getCascadeAction();
	
	 protected Serializable saveWithGeneratedId(
				Object entity,
				String entityName,
				Object anything,
				EventSource source,
				boolean requiresImmediateIdAccess) {
		    //通过实体名取得实体
			EntityPersister persister = source.getEntityPersister( entityName, entity );
			//获取要存储的Id值
			Serializable generatedId = persister.getIdentifierGenerator().generate( source, entity );
			if ( generatedId == null ) {
				throw new IdentifierGenerationException( "null id generated for:" + entity.getClass() );
			}
			else if ( generatedId == IdentifierGeneratorHelper.SHORT_CIRCUIT_INDICATOR ) {
				return source.getIdentifier( entity );
			}
			else if ( generatedId == IdentifierGeneratorHelper.POST_INSERT_INDICATOR ) {
				return performSave( entity, null, persister, true, anything, source, requiresImmediateIdAccess );
			}
			else {
				return performSave( entity, generatedId, persister, false, anything, source, true );
			}
		}

	protected Serializable performSave(
			Object entity, 
			Serializable id,
			EntityPersister persister, 
			boolean useIdentityColumn, 
			Object anything,
			EventSource source, 
			boolean requiresImmediateIdAccess) {

		final EntityKey key;
		if(!useIdentityColumn){
			key = source.generateEntityKey(id, persister);
			Object old = source.getPersistenceContext().getEntity(key);
			if(old != null){
			}
			//通过setter方法设置实体的id值
			persister.setIdentifier( entity, id, source );
		}
		else{
			key = null;
		}
		if ( invokeSaveLifecycle( entity, persister, source ) ) {
			return id; 
		}

		return performSaveOrReplicate(
				entity,
				key,
				persister,
				useIdentityColumn,
				anything,
				source,
				requiresImmediateIdAccess
		);
	}
	
	protected void cascadeBeforeSave(
			EventSource source,
			EntityPersister persister,
			Object entity,
			Object anything) {

		source.getPersistenceContext().incrementCascadeLevel();
		try {
			new Cascade( getCascadeAction(), Cascade.BEFORE_INSERT_AFTER_DELETE, source )
					.cascade( persister, entity, anything );
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}
	
	protected void cascadeAfterSave(
			EventSource source,
			EntityPersister persister,
			Object entity,
			Object anything) {

		// cascade-save to collections AFTER the collection owner was saved
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			new Cascade( getCascadeAction(), Cascade.AFTER_INSERT_BEFORE_DELETE, source )
					.cascade( persister, entity, anything );
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}
	
	protected boolean substituteValuesIfNecessary(
			Object entity,
			Serializable id,
			Object[] values,
			EntityPersister persister,
			SessionImplementor source) {
		boolean substitute = source.getInterceptor().onSave(
				entity,
				id,
				values,
				persister.getPropertyNames(),
				persister.getPropertyTypes()
		);

		//keep the existing version number in the case of replicate!
		if ( persister.isVersioned() ) {
			substitute = Versioning.seedVersion(
					values,
					persister.getVersionProperty(),
					persister.getVersionType(),
					source
			) || substitute;
		}
		return substitute;
	}
	
	protected boolean visitCollectionsBeforeSave(Object entity, Serializable id, Object[] values, Type[] types, EventSource source) {
		WrapVisitor visitor = new WrapVisitor( source );
		visitor.processEntityPropertyValues( values, types );
		return visitor.isSubstitutionRequired();
	}

	private Serializable performSaveOrReplicate(
			Object entity, 
			EntityKey key,
			EntityPersister persister, 
			boolean useIdentityColumn,
			Object anything, 
			EventSource source,
			boolean requiresImmediateIdAccess) {
		Serializable id = key == null ? null : key.getIdentifier();
		boolean inTxn = source.getTransactionCoordinator().isTransactionInProgress();
		boolean shouldDelayIdentityInserts = ! inTxn && !requiresImmediateIdAccess;
		source.getPersistenceContext().addEntry(
				entity,
				Status.SAVING,
				null,
				null,
				id,
				null,
				LockMode.WRITE,
				useIdentityColumn,
				persister,
				false,
				false
		);
		//关联表
		cascadeBeforeSave( source, persister, entity, anything );
		
		//获取需要插入的属性的值
		Object[] values = persister.getPropertyValuesToInsert( entity, getMergeMap( anything ), source );
		//得到持久化类的属性类型，StringType之类的
		Type[] types = persister.getPropertyTypes();
		boolean substitute = substituteValuesIfNecessary( entity, id, values, persister, source );
		if(persister.hasCollections()){
			substitute = substitute || visitCollectionsBeforeSave( entity, id, values, types, source );
		}
		if ( substitute ) {
			persister.setPropertyValues( entity, values );
		}
		
		TypeHelper.deepCopy(
				values,
				types,
				persister.getPropertyUpdateability(),
				values,
				source
		);
		
		//将插入动作加入到动作队列中
		addInsertAction(values, id, entity, persister, useIdentityColumn, source, shouldDelayIdentityInserts);
		
		cascadeAfterSave( source, persister, entity, anything );
		
		return id;
	}
	
	protected EntityState getEntityState(
			Object entity,
			String entityName,
			EntityEntry entry, 
			SessionImplementor source) {

		if ( entry != null ) { 

			if ( entry.getStatus() != Status.DELETED ) {
				return EntityState.PERSISTENT;
			}
			return EntityState.DELETED;
		}

		if ( ForeignKeys.isTransient( entityName, entity, getAssumedUnsaved(), source )) {
			return EntityState.TRANSIENT;
		}
		return EntityState.DETACHED;
	}
	
	protected Boolean getAssumedUnsaved() {
		return null;
	}

	private AbstractEntityInsertAction addInsertAction(
			Object[] values,
			Serializable id, 
			Object entity, 
			EntityPersister persister,
			boolean useIdentityColumn, 
			EventSource source,
			boolean shouldDelayIdentityInserts) {
 
		if(useIdentityColumn){
			return null;
		}
		else{
			Object version = Versioning.getVersion( values, persister );
			EntityInsertAction insert = new EntityInsertAction(
					id, values, entity, version, persister, isVersionIncrementDisabled(), source
			);
			source.getActionQueue().addAction(insert);
			return insert;
		}
	}

	private boolean isVersionIncrementDisabled() {
		return false;
	}

	private Map getMergeMap(Object anything) {
		return null;
	}

	private boolean invokeSaveLifecycle(Object entity,
			EntityPersister persister, EventSource source) {
		return false;
	}

}
