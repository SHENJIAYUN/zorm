package com.zorm.tuple;

import com.zorm.property.Getter;

public interface Tuplizer {
	/*
	 * 获取实体属性值
	 */
	public Object[] getPropertyValues(Object entity);
	/*
	 * 设置实体属性值
	 */
	public void setPropertyValues(Object entity, Object[] values);
    /*
     * 根据索引获取实体某一具体属性值
     */
	public Object getPropertyValue(Object entity, int i);
	/*
	 * 创建一新实体
	 */
	public Object instantiate();
	
	public boolean isInstance(Object object);
	
	public Class getMappedClass();
	/*
	 * 取得实体某一具有属性的getter方法
	 */
	public Getter getGetter(int i);
}


