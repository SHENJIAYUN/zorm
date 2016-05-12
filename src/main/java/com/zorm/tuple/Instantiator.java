package com.zorm.tuple;

import java.io.Serializable;

public interface Instantiator extends Serializable{
	/*
	 * 实例化实体
	 */
	public Object instantiate(Serializable id);
	
	public Object instantiate();
	
	public boolean isInstance(Object object);
}
