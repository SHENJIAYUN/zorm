package com.zorm.engine;

public interface Manageable {
	public String getManagementDomain();

	public String getManagementServiceType();

	public Object getManagementBean();
}
