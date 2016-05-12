package com.zorm.tuple;

import java.io.Serializable;
import java.util.Map;

import com.zorm.EntityNameResolver;
import com.zorm.entity.EntityMode;
import com.zorm.exception.ZormException;
import com.zorm.property.Getter;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;

public interface EntityTuplizer extends Tuplizer{

	/*
	 * 获取entity-mode,默认为pojo
	 */
	public EntityMode getEntityMode();
	/*
	 * 返回实体实例
	 */
	public Object instantiate(Serializable id, SessionImplementor session);
	
	public void setIdentifier(Object entity, Serializable id,SessionImplementor session);

	public Serializable getIdentifier(Object entity);
	/*
	 * 获取实体ID
	 */
	public Serializable getIdentifier(Object entity, SessionImplementor session);
	/*
	 * 使用currentVersion重置实体ID
	 */
	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, SessionImplementor session);

	public Object getVersion(Object entity) throws ZormException;
	/*
	 * 根据索引i设置属性值
	 */
	public void setPropertyValue(Object entity, int i, Object value) throws ZormException;
	/*
	 * 根据属性名设置属性值
	 */
	public void setPropertyValue(Object entity, String propertyName, Object value) throws ZormException;
	/*
	 * 返回要插入数据库的实体的属性值
	 */
	public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap,
			SessionImplementor session);
	
	public Object getPropertyValue(Object entity, String propertyName) throws ZormException;
    /*
     * 仅能在属性初始化后调用
     */
	public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session);

	//public boolean hasProxy();
	
	//public Object createProxy(Serializable id, SessionImplementor session) throws ZormException;

	public boolean isLifecycleImplementor();
	
	public boolean hasUninitializedLazyProperties(Object entity);
	
	public boolean isInstrumented();
	
	public EntityNameResolver[] getEntityNameResolvers();
	
	public String determineConcreteSubclassEntityName(Object entityInstance, SessionFactoryImplementor factory);

	public Getter getIdentifierGetter();
	
	public Getter getVersionGetter();
	
}
