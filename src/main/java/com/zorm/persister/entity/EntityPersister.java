package com.zorm.persister.entity;

import java.io.Serializable;
import java.util.Map;

import com.zorm.FilterAliasGenerator;
import com.zorm.LockOptions;
import com.zorm.engine.CascadeStyle;
import com.zorm.engine.ValueInclusion;
import com.zorm.entity.EntityInstrumentationMetadata;
import com.zorm.entity.EntityMetamodel;
import com.zorm.entity.EntityMode;
import com.zorm.event.EventSource;
import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.id.IdentifierGenerator;
import com.zorm.meta.ClassMetadata;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.tuple.EntityTuplizer;
import com.zorm.type.Type;
import com.zorm.type.VersionType;

public interface EntityPersister extends OptimisticCacheSource {
	public static final String ENTITY_ID = "id";

	/*
	 * 在实体持久化类进行实例化后调用
	 */
	public void postInstantiate() throws MappingException;

	/*
	 * 返回该实体持久化类的sessionFactory
	 */
	public SessionFactoryImplementor getFactory();

	public int getVersionProperty();

	public String getRootEntityName();

	/*
	 * 返回该持久化类映射的实体类名
	 */
	public String getEntityName();

	/*
	 * 返回实体元数据模型实例
	 */
	public EntityMetamodel getEntityMetamodel();

	public boolean isSubclassEntityName(String entityName);

	public Serializable[] getPropertySpaces();

	public boolean hasMutableProperties();

	/*
	 * 判断是否有级联
	 */
	public boolean hasCascades();

	public Type getIdentifierType();

	public ClassMetadata getClassMetadata();

	public IdentifierGenerator getIdentifierGenerator();

	public Serializable getIdentifier(Object obj, SessionImplementor session);

	/*
	 * 有其他实体继承该实体则返回true,否则返回false
	 */
	public boolean isInherited();

	/*
	 * 实体的标识符是在插入的时候产生的则返回true
	 */
	public boolean isIdentifierAssignedByInsert();

	/*
	 * 返回属性名为propertyName的属性类型
	 */
	public Type getPropertyType(String propertyName) throws MappingException;

	public int[] findDirty(Object[] currentState, Object[] previousState,
			Object owner, SessionImplementor session);

	public boolean hasIdentifierProperty();

	public boolean isBatchLoadable();

	/*
	 * 是否启用乐观锁
	 */
	public boolean isVersioned();

	/*
	 * 获取乐观锁类型
	 */
	public VersionType getVersionType();

	public EntityMode getEntityMode();

	public boolean hasNaturalIdentifier();

	public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap,
			SessionImplementor session);

	public Type[] getPropertyTypes();

	public boolean[] getPropertyUpdateability();

	public boolean isMutable();

	public boolean canExtractIdOutOfEntity();

	public Object[] getPropertyValues(Object object);

	public void insert(Serializable id, Object[] state, Object instance,
			SessionImplementor session);

	/**
	 * Persist an instance, using a natively generated identifier (optional
	 * operation)
	 */
	public Serializable insert(Object[] fields, Object object,
			SessionImplementor session) throws ZormException;

	/*
	 * 判断是否有属性配置了延迟加载策略
	 */
	public boolean hasLazyProperties();

	public String[] getPropertyNames();

	public boolean[] getPropertyInsertability();

	public ValueInclusion[] getPropertyInsertGenerationInclusions();

	public boolean[] getPropertyCheckability();

	public boolean[] getPropertyNullability();

	public boolean[] getPropertyVersionability();

	public boolean[] getPropertyLaziness();

	public String getIdentifierPropertyName();

	public boolean isSelectBeforeUpdateRequired();

	public boolean isInstrumented();

	public boolean hasInsertGeneratedProperties();

	public boolean isVersionPropertyGenerated();
	/**
	 * Called just after the entities properties have been initialized
	 */
	public void afterInitialize(Object entity,
			boolean lazyPropertiesAreUnfetched, SessionImplementor session);

	// public void afterReassociate(Object entity, SessionImplementor session);

	public Boolean isTransient(Object object, SessionImplementor session)
			throws ZormException;

	public void processInsertGeneratedProperties(Serializable id,
			Object entity, Object[] state, SessionImplementor session);

	public Class getMappedClass();

	public boolean implementsLifecycle();

	public void setPropertyValues(Object object, Object[] values);

	public void setPropertyValue(Object object, int i, Object value);

	public Object getPropertyValue(Object object, int i) throws ZormException;

	public Object getPropertyValue(Object object, String propertyName);

	public void setIdentifier(Object entity, Serializable id,
			SessionImplementor session);

	public Object getVersion(Object object) throws ZormException;

	public Object instantiate(Serializable id, SessionImplementor session);

	public boolean isInstance(Object object);

	public void resetIdentifier(Object entity, Serializable currentId,
			Object currentVersion, SessionImplementor session);

	public EntityPersister getSubclassEntityPersister(Object instance,
			SessionFactoryImplementor factory);

	public EntityTuplizer getEntityTuplizer();

	public EntityInstrumentationMetadata getInstrumentationMetadata();

	public void delete(Serializable id, Object version, Object object,
			SessionImplementor session);

	public Object load(Serializable id, Object optionalObject,
			LockOptions lockOptions, SessionImplementor session);

	public void update(Serializable id, Object[] state, int[] dirtyFields,
			boolean hasDirtyCollection, Object[] previousState,
			Object previousVersion, Object instance, Object rowId,
			SessionImplementor session);

	public Serializable[] getQuerySpaces();

	public FilterAliasGenerator getFilterAliasGenerator(final String rootAlias);

	public int[] getNaturalIdentifierProperties();

	public int[] findModified(Object[] old, Object[] current, Object object, SessionImplementor session);

	public CascadeStyle[] getPropertyCascadeStyles();

	public boolean hasProxy();

	public boolean hasCollections();

	public Serializable getIdByUniqueKey(Serializable key,
			String lhsPropertyName, SessionImplementor session);

	public Serializable getIdentifier(Object object);

	public boolean hasUninitializedLazyProperties(Object object);

	public Object[] getDatabaseSnapshot(Serializable id,SessionImplementor session);

}
