package com.zorm.session;

import java.io.Serializable;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.naming.Reference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.ConnectionReleaseMode;
import com.zorm.CustomEntityDirtinessStrategy;
import com.zorm.EntityNameResolver;
import com.zorm.Interceptor;
import com.zorm.config.AvailableSettings;
import com.zorm.config.Configuration;
import com.zorm.config.Settings;
import com.zorm.dialect.Dialect;
import com.zorm.dialect.function.SQLFunctionRegistry;
import com.zorm.engine.Mapping;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.ClassLoadingException;
import com.zorm.exception.MappingException;
import com.zorm.exception.ObjectNotFoundException;
import com.zorm.exception.ZormException;
import com.zorm.id.IdentifierGenerator;
import com.zorm.id.IdentifierGeneratorFactory;
import com.zorm.id.UUIDGenerator;
import com.zorm.integrator.Integrator;
import com.zorm.integrator.IntegratorService;
import com.zorm.jdbc.JdbcServices;
import com.zorm.jdbc.SqlExceptionHelper;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.RootClass;
import com.zorm.meta.ClassMetadata;
import com.zorm.meta.CollectionMetadata;
import com.zorm.persister.PersisterFactory;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.persister.entity.Queryable;
import com.zorm.proxy.EntityNotFoundDelegate;
import com.zorm.query.QueryPlanCache;
import com.zorm.service.ClassLoaderService;
import com.zorm.service.ConfigurationService;
import com.zorm.service.ServiceRegistry;
import com.zorm.service.ServiceRegistryImplementor;
import com.zorm.stat.Statistics;
import com.zorm.stat.StatisticsImplementor;
import com.zorm.transaction.TransactionCoordinatorImpl;
import com.zorm.transaction.TransactionEnvironment;
import com.zorm.transaction.TransactionEnvironmentImpl;
import com.zorm.type.Type;
import com.zorm.type.TypeLocatorImpl;
import com.zorm.type.TypeResolver;
import com.zorm.typet.TypeHelper;
import com.zorm.type.AssociationType;
import com.zorm.mapping.Collection;;

@SuppressWarnings("unused")
public final class SessionFactoryImpl implements SessionFactoryImplementor {

	private static final long serialVersionUID = 5500950724585659918L;
	private static final Log log = LogFactory.getLog(SessionFactoryImpl.class);
	
	private static final IdentifierGenerator UUID_GENERATOR = UUIDGenerator.buildSessionFactoryUniqueIdentifierGenerator();

	private final transient SessionFactoryOptions sessionFactoryOptions;
	private final transient Settings settings;
	private final transient Properties properties;
	private final transient SessionFactoryServiceRegistry serviceRegistry;
	private final transient JdbcServices jdbcServices;
	private final transient Dialect dialect;
	private final transient Map<String,CollectionPersister> collectionPersisters;
	private final transient Map<String,CollectionMetadata> collectionMetadata;
	private final transient Map<String,Set<String>> collectionRolesByEntityParticipant;
	private final transient SQLFunctionRegistry sqlFunctionRegistry;
	private final transient SessionFactoryObserverChain observer = new SessionFactoryObserverChain();
	private final transient TypeResolver typeResolver;
	private final transient TypeHelper typeHelper;
	private final transient Map<String,IdentifierGenerator> identifierGenerators;
	private final transient ConcurrentHashMap<EntityNameResolver,Object> entityNameResolvers = new ConcurrentHashMap<EntityNameResolver, Object>();
	private static final Object ENTITY_NAME_RESOLVER_MAP_VALUE = new Object();
	private final transient Map<String,ClassMetadata> classMetadata;
	private final String name;
	private final String uuid;
	private final transient HashMap<String,EntityPersister> entityPersisters;
	private final transient TransactionEnvironment transactionEnvironment;
	private final transient CurrentTenantIdentifierResolver currentTenantIdentifierResolver;
	private final transient CustomEntityDirtinessStrategy customEntityDirtinessStrategy;
	private final transient Map<String,String> imports;
	private final transient QueryPlanCache queryPlanCache;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SessionFactoryImpl(
			final Configuration cfg,
			Mapping mapping,
			ServiceRegistry serviceRegistry,
			Settings settings,
			SessionFactoryObserver observer) throws ZormException{
		log.info("Building session factory");
		
		sessionFactoryOptions = new SessionFactoryOptions() {
			private EntityNotFoundDelegate entityNotFoundDelegate;
			@Override
			public Interceptor getInterceptor() {
				return cfg.getInterceptor();
			}
			
			@Override
			public EntityNotFoundDelegate getEntityNotFoundDelegate() {
				if(entityNotFoundDelegate==null){
					if(cfg.getEntityNotFoundDelegate()!=null){
						entityNotFoundDelegate = cfg.getEntityNotFoundDelegate();
					}
					else{
						entityNotFoundDelegate = new EntityNotFoundDelegate() {
							
							@Override
							public void handleEntityNotFound(String entityName, Serializable id) {
								throw new ObjectNotFoundException(id,entityName);
							}
						};
					}
				}
				
				return entityNotFoundDelegate;
			}
		};
		
		this.settings = settings;
		this.properties = new Properties();
		this.properties.putAll(cfg.getProperties());
		this.serviceRegistry = serviceRegistry.getService(SessionFactoryServiceRegistryFactory.class).buildServiceRegistry(this, cfg);
	    this.jdbcServices = this.serviceRegistry.getService(JdbcServices.class);
	    this.dialect = this.jdbcServices.getDialect();
	    this.sqlFunctionRegistry = new SQLFunctionRegistry(getDialect(),cfg.getSqlFunctions());
	    if(observer!=null){
	    	this.observer.addObserver(observer);
	    }
	    this.typeResolver = cfg.getTypeResolver().scope(this);
	    this.typeHelper=new TypeLocatorImpl( typeResolver );
	    
	    log.debug("Instantiating session factory with properties:"+properties.toString());
	    
	    this.queryPlanCache = new QueryPlanCache(this);
	    
	    @SuppressWarnings("serial")
		class IntegratorObserver implements SessionFactoryObserver {
			private ArrayList<Integrator> integrators = new ArrayList<Integrator>();

			@Override
			public void sessionFactoryCreated(SessionFactory factory) {
			}

			@Override
			public void sessionFactoryClosed(SessionFactory factory) {
				for ( Integrator integrator : integrators ) {
					integrator.disintegrate( SessionFactoryImpl.this, SessionFactoryImpl.this.serviceRegistry );
				}
			}
		}
	    final IntegratorObserver integratorObserver = new IntegratorObserver();
		this.observer.addObserver( integratorObserver );
		for ( Integrator integrator : serviceRegistry.getService( IntegratorService.class ).getIntegrators() ) {
			integrator.integrate( cfg, this, this.serviceRegistry );
			integratorObserver.integrators.add( integrator );
		}
		
		//id生成器
		identifierGenerators = new HashMap();
		//获取所有的PersistentClass
		Iterator classes = cfg.getClassMappings();
		while ( classes.hasNext() ) {
			PersistentClass model = (PersistentClass) classes.next();
			if ( !model.isInherited() ) {
				IdentifierGenerator generator = model.getIdentifier().createIdentifierGenerator(
						cfg.getIdentifierGeneratorFactory(),
						getDialect(),
				        settings.getDefaultCatalogName(),
				        settings.getDefaultSchemaName(),
				        (RootClass) model
				);
				identifierGenerators.put( model.getEntityName(), generator );
			}
		}

		entityPersisters = new HashMap();
		Map<String,ClassMetadata> classMeta = new HashMap<String,ClassMetadata>();
		classes = cfg.getClassMappings();
		while ( classes.hasNext() ) {
			final PersistentClass model = (PersistentClass) classes.next();
	
			EntityPersister cp = serviceRegistry.getService( PersisterFactory.class ).createEntityPersister(
					model,
					this,
					mapping
			);
			entityPersisters.put( model.getEntityName(), cp );
			classMeta.put( model.getEntityName(), cp.getClassMetadata() );
		}
		classMetadata = Collections.unmodifiableMap(classMeta);
		Map<String,Set<String>> tmpEntityToCollectionRoleMap = new HashMap<String,Set<String>>();
		Map<String,CollectionMetadata> tmpCollectionMetadata = new HashMap<String,CollectionMetadata>();
		collectionPersisters = new HashMap<String,CollectionPersister>();
		
		Iterator collections = cfg.getCollectionMappings();
		while(collections.hasNext()){
			Collection model = (Collection)collections.next();
            CollectionPersister persister = serviceRegistry.getService(PersisterFactory.class).createCollectionPersister(model, cfg, this);
            collectionPersisters.put(model.getRole(), persister);
            tmpCollectionMetadata.put( model.getRole(), persister.getCollectionMetadata() );
            Type indexType = persister.getIndexType();
			if ( indexType != null && indexType.isAssociationType() && !indexType.isAnyType() ) {
				String entityName = ( ( AssociationType ) indexType ).getAssociatedEntityName( this );
				Set roles = tmpEntityToCollectionRoleMap.get( entityName );
				if ( roles == null ) {
					roles = new HashSet();
					tmpEntityToCollectionRoleMap.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
			Type elementType = persister.getElementType();
			if ( elementType.isAssociationType() && !elementType.isAnyType() ) {
				String entityName = ( ( AssociationType ) elementType ).getAssociatedEntityName( this );
				Set roles = tmpEntityToCollectionRoleMap.get( entityName );
				if ( roles == null ) {
					roles = new HashSet();
					tmpEntityToCollectionRoleMap.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
		}
		
		collectionMetadata = Collections.unmodifiableMap( tmpCollectionMetadata );
		Iterator itr = tmpEntityToCollectionRoleMap.entrySet().iterator();
		while ( itr.hasNext() ) {
			final Map.Entry entry = ( Map.Entry ) itr.next();
			entry.setValue( Collections.unmodifiableSet( ( Set ) entry.getValue() ) );
		}
		collectionRolesByEntityParticipant = Collections.unmodifiableMap( tmpEntityToCollectionRoleMap );
		
		//Named Queries
		imports = new HashMap<String,String>( cfg.getImports() );

		Iterator iter = entityPersisters.values().iterator();
		while ( iter.hasNext() ) {
			final EntityPersister persister = ( ( EntityPersister ) iter.next() );
			persister.postInstantiate();
		}
		
		iter = collectionPersisters.values().iterator();
		while ( iter.hasNext() ) {
			final CollectionPersister persister = ( ( CollectionPersister ) iter.next() );
			persister.postInstantiate();
		}

		//JNDI + Serialization:

		name = settings.getSessionFactoryName();
		try {
			uuid = (String) UUID_GENERATOR.generate(null, null);
		}
		catch (Exception e) {
			throw new AssertionFailure("Could not generate UUID");
		}
		
		SessionFactoryRegistry.INSTANCE.addSessionFactory(
				uuid,
				name,
				settings.isSessionFactoryNameAlsoJndiName(),
				this
		);
		
		log.info("Instantiated session factory");

		this.customEntityDirtinessStrategy = determineCustomEntityDirtinessStrategy();
		this.currentTenantIdentifierResolver = determineCurrentTenantIdentifierResolver( cfg.getCurrentTenantIdentifierResolver() );
		this.transactionEnvironment = new TransactionEnvironmentImpl( this );
		this.observer.sessionFactoryCreated( this );
	}
	
	@Override
	public Properties getProperties() {
		return properties;
	}

	public CollectionPersister getCollectionPersister(String role) throws MappingException {
		CollectionPersister result = collectionPersisters.get(role);
		if ( result == null ) {
			throw new MappingException( "Unknown collection role: " + role );
		}
		return result;
	}
	
	private CustomEntityDirtinessStrategy determineCustomEntityDirtinessStrategy() {
		CustomEntityDirtinessStrategy defaultValue = new CustomEntityDirtinessStrategy() {

			@Override
			public void findDirty(
					Object entity,
					EntityPersister persister,
					Session session,
					DirtyCheckContext dirtyCheckContext) {
				// todo : implement proper method body
			}

			@Override
			public void resetDirty(Object entity, EntityPersister persister,
					Session session) {
			}
		};
		return serviceRegistry.getService( ConfigurationService.class ).getSetting(
				AvailableSettings.CUSTOM_ENTITY_DIRTINESS_STRATEGY,
				CustomEntityDirtinessStrategy.class,
				defaultValue
		);
	}

	private CurrentTenantIdentifierResolver determineCurrentTenantIdentifierResolver(
			CurrentTenantIdentifierResolver explicitResolver) {
		if ( explicitResolver != null ) {
			return explicitResolver;
		}
		return serviceRegistry.getService( ConfigurationService.class )
				.getSetting(
						AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
						CurrentTenantIdentifierResolver.class,
						null
				);
	}


	public Dialect getDialect() {
        if(serviceRegistry==null){
        	throw new IllegalStateException( "Cannot determine dialect because serviceRegistry is null.");
        }
		return dialect;
	}


	public IdentifierGenerator getIdentifierGenerator(String rootEntityName) {
		return identifierGenerators.get(rootEntityName);
	}
	
	@Override
	public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return null;
	}

	public SqlExceptionHelper getSQLExceptionHelper() {
		return getJdbcServices().getSqlExceptionHelper();
	}
	
	@Override
	public String[] getImplementors(String className) throws MappingException {
        final Class clazz;
        try{
        	clazz = serviceRegistry.getService( ClassLoaderService.class ).classForName( className );
        }
        catch(ClassLoadingException e){
        	return new String[]{className};
        }
        ArrayList<String> results = new ArrayList<String>();
        for ( EntityPersister checkPersister : entityPersisters.values() ) {
			if ( ! Queryable.class.isInstance( checkPersister ) ) {
				continue;
			}
			final Queryable checkQueryable = Queryable.class.cast( checkPersister );
			final String checkQueryableEntityName = checkQueryable.getEntityName();
			final boolean isMappedClass = className.equals( checkQueryableEntityName );
			if ( checkQueryable.isExplicitPolymorphism() ) {
				if ( isMappedClass ) {
					return new String[] { className }; //NOTE EARLY EXIT
				}
			}
			else {
				if ( isMappedClass ) {
					results.add( checkQueryableEntityName );
				}
				else {
					final Class mappedClass = checkQueryable.getMappedClass();
					if ( mappedClass != null && clazz.isAssignableFrom( mappedClass ) ) {
						final boolean assignableSuperclass;
						if ( checkQueryable.isInherited() ) {
							Class mappedSuperclass = getEntityPersister( checkQueryable.getMappedSuperclass() ).getMappedClass();
							assignableSuperclass = clazz.isAssignableFrom( mappedSuperclass );
						}
						else {
							assignableSuperclass = false;
						}
						if ( !assignableSuperclass ) {
							results.add( checkQueryableEntityName );
						}
					}
				}
			}
		}
		return results.toArray( new String[results.size()] );
	}

	@Override
	public String getImportedClassName(String className) {
        String result = imports.get(className);
        if(result == null){
        	try{
        		serviceRegistry.getService(ClassLoaderService.class).classForName(className);
        		return className;
        	}
        	catch(ClassLoadingException e){
        		return null;
        	}
        }else{
		   return result;
        }
	}

	@Override
	public String getIdentifierPropertyName(String className)
			throws MappingException {
		return null;
	}
	
	public Session openSession() throws ZormException{
		return withOptions().openSession();
	}
	
	@Override
	public SessionBuilderImplementor withOptions() {
		return new SessionBuilderImpl( this );
	}


	@Override
	public Type getReferencedPropertyType(String className, String propertyName)
			throws MappingException {
		return null;
	}


	@Override
	public SessionFactoryOptions getSessionFactoryOptions() {
		return null;
	}


	@Override
	public Reference getReference() throws NamingException {
		return null;
	}		
	
	static class SessionBuilderImpl implements SessionBuilderImplementor {
		private final SessionFactoryImpl sessionFactory;
		private SessionOwner sessionOwner;
		private Interceptor interceptor;
		private Connection connection;
		private ConnectionReleaseMode connectionReleaseMode;
		private boolean autoClose;
		private boolean autoJoinTransactions = true;
		private boolean flushBeforeCompletion;
		private String tenantIdentifier;

		SessionBuilderImpl(SessionFactoryImpl sessionFactory) {
			this.sessionFactory = sessionFactory;
			this.sessionOwner = null;
			final Settings settings = sessionFactory.settings;

			// set up default builder values...
			this.interceptor = sessionFactory.getInterceptor();
			//默认是ON_CLOSE,必须session显示关闭，连接才能关闭
			this.connectionReleaseMode = settings.getConnectionReleaseMode();
			//默认是false
			this.autoClose = settings.isAutoCloseSessionEnabled();
			this.flushBeforeCompletion = settings.isFlushBeforeCompletionEnabled();

			if ( sessionFactory.getCurrentTenantIdentifierResolver() != null ) {
				tenantIdentifier = sessionFactory.getCurrentTenantIdentifierResolver().resolveCurrentTenantIdentifier();
			}
		}

		@Override
		public Session openSession() {
			return new SessionImpl(
					connection,
					sessionFactory,
					sessionOwner,
					getTransactionCoordinator(),
					autoJoinTransactions,
					System.currentTimeMillis() / 100,
					interceptor,
					flushBeforeCompletion,
					autoClose,
					connectionReleaseMode,
					tenantIdentifier
			);
		}

		private TransactionCoordinatorImpl getTransactionCoordinator() {
			return null;
		}

		@Override
		public SessionBuilder owner(SessionOwner sessionOwner) {
			this.sessionOwner = sessionOwner;
			return this;
		}

	}

	public Interceptor getInterceptor() {
		return sessionFactoryOptions.getInterceptor();
	}

    @Override
	public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
		return currentTenantIdentifierResolver;
	}


	public Settings getSettings() {
		return settings;
	}


	public Iterable<EntityNameResolver> iterateEntityNameResolvers() {
		return entityNameResolvers.keySet();
	}


	public TransactionEnvironment getTransactionEnvironment() {
		return transactionEnvironment;
	}

	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	public ServiceRegistryImplementor getServiceRegistry() {
		return serviceRegistry;
	}


	public Statistics getStatistics() {
		return getStatisticsImplementor();
	}


	public StatisticsImplementor getStatisticsImplementor() {
		return serviceRegistry.getService( StatisticsImplementor.class );
	}

	public TypeResolver getTypeResolver() {
		return typeResolver;
	}

	
	
	@Override
	public EntityPersister getEntityPersister(String entityName)
			throws MappingException {
        EntityPersister result = entityPersisters.get(entityName);
        if(result == null){
        	throw new MappingException( "Unknown entity: " + entityName );
        }
		return result;
	}


	@Override
	public SQLFunctionRegistry getSqlFunctionRegistry() {
		return sqlFunctionRegistry;
	}


	@Override
	public Map<String, ClassMetadata> getAllClassMetadata() {
		return null;
	}


	@Override
	public  CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
		return customEntityDirtinessStrategy;
	}

	@Override
	public QueryPlanCache getQueryPlanCache() {
		return queryPlanCache;
	}
	
	public Type getIdentifierType(String className) throws MappingException {
		return getEntityPersister(className).getIdentifierType();
	}

}
