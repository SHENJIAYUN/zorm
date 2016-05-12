package com.zorm.stat;

import com.zorm.service.Service;

public interface StatisticsImplementor extends Statistics,Service{

	public void deleteEntity(String entityName);

	public void flush();

	public void optimisticFailure(String entityName);


}
