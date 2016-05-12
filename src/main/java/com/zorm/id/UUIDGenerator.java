package com.zorm.id;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.dialect.Dialect;
import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;
import com.zorm.type.UUIDTypeDescriptor;
import com.zorm.util.ReflectHelper;

public class UUIDGenerator implements IdentifierGenerator,Configurable{
	public static final String UUID_GEN_STRATEGY = "uuid_gen_strategy";
	public static final String UUID_GEN_STRATEGY_CLASS = "uuid_gen_strategy_class";
	private static final Log log = LogFactory.getLog(UUIDGenerator.class);
	
	private UUIDGenerationStrategy strategy;
	private UUIDTypeDescriptor.ValueTransformer valueTransformer;
	
	public static UUIDGenerator buildSessionFactoryUniqueIdentifierGenerator() {
		final UUIDGenerator generator = new UUIDGenerator();
		generator.strategy = StandardRandomStrategy.INSTANCE;
		generator.valueTransformer = UUIDTypeDescriptor.ToStringTransformer.INSTANCE;
		return generator;
	}
	
	public void configure(Type type, Properties params, Dialect d) throws MappingException {
		// check first for the strategy instance
		strategy = (UUIDGenerationStrategy) params.get( UUID_GEN_STRATEGY );
		if ( strategy == null ) {
			// next check for the strategy class
			final String strategyClassName = params.getProperty( UUID_GEN_STRATEGY_CLASS );
			if ( strategyClassName != null ) {
				try {
					final Class strategyClass = ReflectHelper.classForName( strategyClassName );
					try {
						strategy = (UUIDGenerationStrategy) strategyClass.newInstance();
					}
					catch ( Exception ignore ) {
                        log.warn("Unable to instantiate UUID generation strategy class :"+ignore);
					}
				}
				catch ( ClassNotFoundException ignore ) {
					log.warn("Unable to locate requested UUID generation strategy class :"+strategyClassName);
				}
			}
		}
		if ( strategy == null ) {
			// lastly use the standard random generator
			strategy = StandardRandomStrategy.INSTANCE;
		}

		if ( UUID.class.isAssignableFrom( type.getReturnedClass() ) ) {
			valueTransformer = UUIDTypeDescriptor.PassThroughTransformer.INSTANCE;
		}
		else if ( String.class.isAssignableFrom( type.getReturnedClass() ) ) {
			valueTransformer = UUIDTypeDescriptor.ToStringTransformer.INSTANCE;
		}
		else if ( byte[].class.isAssignableFrom( type.getReturnedClass() ) ) {
			valueTransformer = UUIDTypeDescriptor.ToBytesTransformer.INSTANCE;
		}
		else {
			throw new ZormException( "Unanticipated return type [" + type.getReturnedClass().getName() + "] for UUID conversion" );
		}
	}

	public Serializable generate(SessionImplementor session, Object object) throws ZormException {
		return valueTransformer.transform( strategy.generateUUID( session ) );
	}

}
