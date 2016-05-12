package com.zorm.session;

import java.io.Serializable;
import java.sql.Connection;

import com.zorm.exception.ZormException;

public interface Session extends SharedSessionContract{

	//保存操作
	public Serializable save(Object object);
	public Serializable save(String entityName, Object object);
    //删除操作
	public void delete(Object object);
	//public void delete(String entityName, Object object);
	//查找操作
	public Object find(Class clazz,Serializable id);
	//更新操作
	public void update(Object object);
	public void update(String entityName, Object object);
	
	public void saveOrUpdate(String entityName, Object object);
	
	public Serializable getIdentifier(Object entity);
	
	public Connection close();
}
