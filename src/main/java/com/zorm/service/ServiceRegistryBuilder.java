package com.zorm.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zorm.config.Environment;
import com.zorm.integrator.Integrator;
import com.zorm.integrator.IntegratorService;
import com.zorm.integrator.ServiceContributingIntegrator;
import com.zorm.util.ConfigurationHelper;

public class ServiceRegistryBuilder {
  //默认的配置文件
  public static final String DEFAULT_CGF_RESOURCE_NAME="config.xml";
  private final Map settings;
  private final List<BasicServiceInitiator> initiator = standardInitiatorList();
  private final List<ProvidedService> providedServices = new ArrayList<ProvidedService>();
  //继承自ServiceRegistry
  private final BootstrapServiceRegistry bootstrapServiceRegistry;
  
  //创建默认ServiceRegistryBuilder
  public ServiceRegistryBuilder(){
	  //引导服务
	  this(new BootstrapServiceRegistryImpl());
  }
  
  public ServiceRegistryBuilder(BootstrapServiceRegistry bootstrapServiceRegistry) {
        this.settings = Environment.getProperties();
        this.bootstrapServiceRegistry = bootstrapServiceRegistry;
  }

 private List<BasicServiceInitiator> standardInitiatorList() {
	final List<BasicServiceInitiator> initiators = new ArrayList<BasicServiceInitiator>();
    //添加各种基础服务
	initiators.addAll( StandardServiceInitiators.LIST );
	return initiators;
  }
 
 //添加设置
 public ServiceRegistryBuilder applySetting(String settingName,Object value){
	 settings.put(settingName, value);
	 return this;
 }
 
 public ServiceRegistryBuilder applySetting(Map settings){
	 this.settings.putAll(settings);
	 return this;
 }
 
 public ServiceRegistry buildServiceRegistry(){
	 Map<?,?> settingsCopy = new HashMap();
	 settingsCopy.putAll(settings);
	 Environment.verifyProperties(settingsCopy);
	 ConfigurationHelper.resolvePlaceHolders(settingsCopy);
	 
	 for(Integrator integrator : bootstrapServiceRegistry.getService(IntegratorService.class).getIntegrators() ){
		//预备服务 
		 if ( ServiceContributingIntegrator.class.isInstance( integrator ) ) {
				ServiceContributingIntegrator.class.cast( integrator ).prepareServices( this );
		 }
	 }
	 return new StandardServiceRegistryImpl(bootstrapServiceRegistry, initiator, providedServices, settingsCopy );
 }

}
