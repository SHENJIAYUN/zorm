package com.zorm.type;

public interface BasicType extends Type{
	/**
	 * 获取类型在注册器中注册的名字
	 */
  public String[] getRegistrationKeys();
}
