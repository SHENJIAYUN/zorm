package com.zorm.type;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.exception.ZormException;

public class BasicTypeRegistry implements Serializable{
  private static final Log log = LogFactory.getLog(BasicTypeRegistry.class);
  private boolean locked = false;
  private Map<String,BasicType> registry = new ConcurrentHashMap<String, BasicType>( 100, .75f, 1 );

  public BasicTypeRegistry() {
	  //添加基础类型
		register( IntegerType.INSTANCE );
	    register( StringType.INSTANCE );
	}
  
private void register(BasicType type) {
	if ( locked ) {
		throw new ZormException( "Can not alter TypeRegistry at this time" );
	}
	if ( type == null ) {
		throw new ZormException( "Type to register cannot be null" );
	}
	if(type.getRegistrationKeys()==null || type.getRegistrationKeys().length==0){
		log.warn("Type"+type.toString()+" defined no registration keys; ignoring");
	}
	for(String key : type.getRegistrationKeys()){
		if(key==null) continue;
		log.debug("Adding type registration"+key+" -> "+type.toString());
		//注册类型
		//registry存储了ZORM的所有类型
		registry.put(key, type);
	}
}

private BasicTypeRegistry(Map<String, BasicType> registry) {
  registry.putAll(registry);
  locked = true;
}

public BasicTypeRegistry shallowCopy() {
	return new BasicTypeRegistry(this.registry);
}

public BasicType getRegisteredType(String key) {
	return registry.get(key);
}
}
