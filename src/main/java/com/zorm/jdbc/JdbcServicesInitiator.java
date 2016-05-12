package com.zorm.jdbc;

import java.util.Map;

import com.zorm.service.BasicServiceInitiator;
import com.zorm.service.ServiceRegistryImplementor;

public class JdbcServicesInitiator implements BasicServiceInitiator<JdbcServices>{
  public static final JdbcServicesInitiator INSTANCE = new JdbcServicesInitiator();

  @Override
  public Class<JdbcServices> getServiceInitiated(){
	  return JdbcServices.class;
  }
  
  @Override
	public JdbcServices initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new JdbcServicesImpl();
	}

}
