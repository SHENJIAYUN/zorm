package com.zorm.id;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.dialect.Dialect;
import com.zorm.exception.MappingException;
import com.zorm.service.ServiceRegistryAwareService;
import com.zorm.service.ServiceRegistryImplementor;
import com.zorm.type.Type;
import com.zorm.util.ReflectHelper;

public class DefaultIdentifierGeneratorFactory implements MutableIdentifierGeneratorFactory, Serializable, ServiceRegistryAwareService{

	private static final Log log = LogFactory.getLog(DefaultIdentifierGeneratorFactory.class);
	
	private transient Dialect dialect;
	private ConcurrentHashMap<String, Class> generatorStrategyToClassNameMap = new ConcurrentHashMap<String, Class>();;

	public DefaultIdentifierGeneratorFactory() {
		//注册ID生成策略
		register( "assigned", Assigned.class );
	}
	
	@Override
	public Dialect getDialect() {
		return dialect;
	}

	@Override
	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public IdentifierGenerator createIdentifierGenerator(String strategy,
			Type type, Properties config) {
		try {
			Class clazz = getIdentifierGeneratorClass( strategy );
			IdentifierGenerator identifierGenerator = ( IdentifierGenerator ) clazz.newInstance();
			if ( identifierGenerator instanceof Configurable ) {
				( ( Configurable ) identifierGenerator ).configure( type, config, dialect );
			}
			return identifierGenerator;
		}
		catch ( Exception e ) {
			final String entityName = config.getProperty( IdentifierGenerator.ENTITY_NAME );
			throw new MappingException( String.format( "Could not instantiate id generator [entity-name=%s]", entityName ), e );
		}
	}

	@Override
	public Class getIdentifierGeneratorClass(String strategy) {
		if ( "native".equals( strategy ) ) {
			return dialect.getNativeIdentifierGeneratorClass();
		}

		Class generatorClass = generatorStrategyToClassNameMap.get( strategy );
		try {
			if ( generatorClass == null ) {
				generatorClass = ReflectHelper.classForName( strategy );
			}
		}
		catch ( ClassNotFoundException e ) {
			throw new MappingException( String.format( "Could not interpret id generator strategy [%s]", strategy ) );
		}
		return generatorClass;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void register(String strategy, Class generatorClass) {
		final Class previous = generatorStrategyToClassNameMap.put( strategy, generatorClass );
		if ( previous != null ) {
			log.debug( "    - overriding [%s]"+previous.getName() );
		}
	}

}
