package com.zorm.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.persistence.Embeddable;
import javax.persistence.Entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.zorm.EmptyInterceptor;
import com.zorm.Interceptor;
import com.zorm.annotations.reflection.JPAMetadataProvider;
import com.zorm.annotations.reflection.JavaReflectionManager;
import com.zorm.annotations.reflection.MetadataProvider;
import com.zorm.annotations.reflection.MetadataProviderInjector;
import com.zorm.annotations.reflection.ReflectionManager;
import com.zorm.annotations.reflection.XClass;
import com.zorm.dialect.function.SQLFunction;
import com.zorm.engine.Mapping;
import com.zorm.engine.ResultSetMappingDefinition;
import com.zorm.entity.EntityTuplizerFactory;
import com.zorm.exception.AnnotationException;
import com.zorm.exception.DuplicateMappingException;
import com.zorm.exception.MappingException;
import com.zorm.exception.RecoverableException;
import com.zorm.exception.ZormException;
import com.zorm.id.DefaultIdentifierGeneratorFactory;
import com.zorm.id.IdentifierGeneratorFactory;
import com.zorm.id.MutableIdentifierGeneratorFactory;
import com.zorm.mapping.Collection;
import com.zorm.mapping.Column;
import com.zorm.mapping.DenormalizedTable;
import com.zorm.mapping.ForeignKey;
import com.zorm.mapping.Join;
import com.zorm.mapping.MappedSuperclass;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Property;
import com.zorm.mapping.Table;
import com.zorm.proxy.EntityNotFoundDelegate;
import com.zorm.service.ServiceRegistry;
import com.zorm.session.CurrentTenantIdentifierResolver;
import com.zorm.session.SessionFactory;
import com.zorm.session.SessionFactoryImpl;
import com.zorm.session.SessionFactoryObserver;
import com.zorm.type.Type;
import com.zorm.type.TypeDef;
import com.zorm.type.TypeResolver;
import com.zorm.util.ConfigHelper;
import com.zorm.util.ConfigurationHelper;
import com.zorm.util.ReflectHelper;
import com.zorm.util.StringHelper;
import com.zorm.util.XMLErrorLogger;
import com.zorm.util.XMLHelper;
import com.zorm.mapping.SimpleValue;

/**
 * 
 * @author JIA
 * 系统的配置环境
 *
 */
public class Configuration implements Serializable{

	private static final long serialVersionUID = 3212539887988699495L;
	public static final String ARTEFACT_PROCESSING_ORDER = "zorm.mapping.precedence";
	private static final Log log = LogFactory.getLog(Configuration.class);
	
    private Properties properties;
    protected List<SecondPass> secondPasses;
    private transient ReflectionManager reflectionManager;
    protected MetadataSourceQueue metadataSourceQueue;
    private TypeResolver typeResolver = new TypeResolver();
    protected Map<String, Table> tables;
    protected Map<String, ResultSetMappingDefinition> sqlResultSetMappings;
    protected Map columnNameBindingPerTable;
	private Interceptor interceptor;
    protected transient XMLHelper xmlHelper;
    private Map<String, AnnotatedClassType> classTypes;
	protected final SettingsFactory settingsFactory;
	protected NamingStrategy namingStrategy;
	private transient Mapping mapping = buildMapping();
	private MutableIdentifierGeneratorFactory identifierGeneratorFactory;
	protected Map<String, PersistentClass> classes;
	protected Map<String, String> imports;
	private boolean specjProprietarySyntaxEnabled;
	private boolean inSecondPass = false;
	private Map<XClass, Map<String, PropertyData>> propertiesAnnotatedWithMapsId;
	private SessionFactoryObserver sessionFactoryObserver;
	protected Map<String, SQLFunction> sqlFunctions;
	protected Map tableNameBinding;
	protected List<Mappings.PropertyReference> propertyReferences;
	protected Map<String, TypeDef> typeDefs;
	private EntityTuplizerFactory entityTuplizerFactory;
	private Map<String, Map<String, Join>> joins;
	private Map<String, String> mappedByResolver;
	private Map<String, String> propertyRefResolver;
	protected Map<String, Collection> collections;
	private List<MetadataSourceType> metadataSourcePrecedence;
	private EntityNotFoundDelegate entityNotFoundDelegate;
	private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;
	
	public Configuration() {
      this(new SettingsFactory());
	}

	public SessionFactory buildSessionFactory(ServiceRegistry serviceRegistry) throws ZormException{
		log.debug("Preparing to build session factory with filters.");
		//解析注解，进行类和数据库之间的映射
		secondPassCompile();
		
        Environment.verifyProperties(properties);
        Properties copy = new Properties();
        copy.putAll(properties);
        ConfigurationHelper.resolvePlaceHolders(copy);
        Settings settings = buildSettings( copy, serviceRegistry );
		return new SessionFactoryImpl(
				this,
				mapping,
				serviceRegistry,
				settings,
				sessionFactoryObserver
			);
	}
	
	public Settings buildSettings(Properties props, ServiceRegistry serviceRegistry) {
		return buildSettingsInternal(props,serviceRegistry);
	}

	private Settings buildSettingsInternal(Properties props, ServiceRegistry serviceRegistry) {
		final Settings settings = settingsFactory.buildSettings(props,serviceRegistry);
		settings.setEntityTuplizerFactory( this.getEntityTuplizerFactory() );
		return settings;
	}

	private EntityTuplizerFactory getEntityTuplizerFactory() {
		return entityTuplizerFactory;
	}
	
	protected void secondPassCompile() throws MappingException{
		log.info("Starting secondPassCompile() processing");
		{
			//将有注解@Entity放在annotatedClassesByEntityNameMap中
			metadataSourceQueue.syncAnnotatedClasses();
			metadataSourceQueue.processMetadata(determineMetadataSourcePrecedence());
		}
		
		try{
			inSecondPass = true;
			//设置SimpleValue的TypeName
			processSecondPassesOfType( SetSimpleValueTypeSecondPass.class );
			processFkSecondPassInOrder();
			processSecondPassesOfType( CreateKeySecondPass.class );
			processSecondPassesOfType( SecondaryTableSecondPass.class );

			originalSecondPassCompile();
			
			inSecondPass = false;
		}
		catch(Exception e){
			throw ( RuntimeException ) e.getCause();
		}
	}
	
	private List<FkSecondPass> getFKSecondPassesOnly() {
		Iterator iter = secondPasses.iterator();
		List<FkSecondPass> fkSecondPasses = new ArrayList<FkSecondPass>( secondPasses.size() );
		while ( iter.hasNext() ) {
			SecondPass sp = ( SecondPass ) iter.next();
			if ( sp instanceof FkSecondPass ) {
				fkSecondPasses.add( ( FkSecondPass ) sp );
				iter.remove();
			}
		}
		return fkSecondPasses;
	}
	
	private String quotedTableName(Table table) {
		return Table.qualify( table.getCatalog(), table.getQuotedSchema(), table.getQuotedName() );
	}
	
	private void buildRecursiveOrderedFkSecondPasses(
			List<FkSecondPass> orderedFkSecondPasses,
			Map<String, Set<FkSecondPass>> isADependencyOf,
			String startTable,
			String currentTable) {

		Set<FkSecondPass> dependencies = isADependencyOf.get( currentTable );

		if ( dependencies == null || dependencies.size() == 0 ) {
			return;
		}

		for ( FkSecondPass sp : dependencies ) {
			String dependentTable = quotedTableName(sp.getValue().getTable());
			if ( dependentTable.compareTo( startTable ) == 0 ) {
				StringBuilder sb = new StringBuilder(
						"Foreign key circularity dependency involving the following tables: "
				);
				throw new AnnotationException( sb.toString() );
			}
			buildRecursiveOrderedFkSecondPasses( orderedFkSecondPasses, isADependencyOf, startTable, dependentTable );
			if ( !orderedFkSecondPasses.contains( sp ) ) {
				orderedFkSecondPasses.add( 0, sp );
			}
		}
	}
	
	private void processFkSecondPassInOrder() {
		List<FkSecondPass> fkSecondPasses = getFKSecondPassesOnly();
		
		if ( fkSecondPasses.size() == 0 ) {
			return; // nothing to do here
		}
		
		Map<String, Set<FkSecondPass>> isADependencyOf = new HashMap<String, Set<FkSecondPass>>();
		List<FkSecondPass> endOfQueueFkSecondPasses = new ArrayList<FkSecondPass>( fkSecondPasses.size() );
		for ( FkSecondPass sp : fkSecondPasses ) {
			if ( sp.isInPrimaryKey() ) {
				String referenceEntityName = sp.getReferencedEntityName();
				PersistentClass classMapping = getClassMapping( referenceEntityName );
				String dependentTable = quotedTableName(classMapping.getTable());
				if ( !isADependencyOf.containsKey( dependentTable ) ) {
					isADependencyOf.put( dependentTable, new HashSet<FkSecondPass>() );
				}
				isADependencyOf.get( dependentTable ).add( sp );
			}
			else {
				endOfQueueFkSecondPasses.add( sp );
			}
		}

		List<FkSecondPass> orderedFkSecondPasses = new ArrayList<FkSecondPass>( fkSecondPasses.size() );
		for ( String tableName : isADependencyOf.keySet() ) {
			buildRecursiveOrderedFkSecondPasses( orderedFkSecondPasses, isADependencyOf, tableName, tableName );
		}

		// process the ordered FkSecondPasses
		for ( FkSecondPass sp : orderedFkSecondPasses ) {
			sp.doSecondPass( classes );
		}

		processEndOfQueue( endOfQueueFkSecondPasses );
	}
	
	private void processEndOfQueue(List<FkSecondPass> endOfQueueFkSecondPasses) {
		boolean stopProcess = false;
		RuntimeException originalException = null;
		while ( !stopProcess ) {
			List<FkSecondPass> failingSecondPasses = new ArrayList<FkSecondPass>();
			for ( FkSecondPass pass : endOfQueueFkSecondPasses ) {
				try {
					pass.doSecondPass( classes );
				}
				catch (RecoverableException e) {
					failingSecondPasses.add( pass );
					if ( originalException == null ) {
						originalException = (RuntimeException) e.getCause();
					}
				}
			}
			stopProcess = failingSecondPasses.size() == 0 || failingSecondPasses.size() == endOfQueueFkSecondPasses.size();
			endOfQueueFkSecondPasses = failingSecondPasses;
		}
		if ( endOfQueueFkSecondPasses.size() > 0 ) {
			throw originalException;
		}
	}
	
	private void originalSecondPassCompile() throws MappingException {

		Iterator itr = secondPasses.iterator();
		while ( itr.hasNext() ) {
			SecondPass sp = (SecondPass) itr.next();
			if ( ! (sp instanceof QuerySecondPass) ) {
				sp.doSecondPass( classes );
				itr.remove();
			}
		}

		itr = secondPasses.iterator();
		while ( itr.hasNext() ) {
			SecondPass sp = (SecondPass) itr.next();
			sp.doSecondPass( classes );
			itr.remove();
		}

		itr = propertyReferences.iterator();
		while ( itr.hasNext() ) {
			Mappings.PropertyReference upr = (Mappings.PropertyReference) itr.next();

			PersistentClass clazz = getClassMapping( upr.referencedClass );
			if ( clazz == null ) {
				throw new MappingException(
						"property-ref to unmapped class: " +
						upr.referencedClass
					);
			}

			Property prop = clazz.getReferencedProperty( upr.propertyName );
			if ( upr.unique ) {
				( (SimpleValue) prop.getValue() ).setAlternateUniqueKey( true );
			}
		}


		itr = getTableMappings();
		Set<ForeignKey> done = new HashSet<ForeignKey>();
		while ( itr.hasNext() ) {
			secondPassCompileForeignKeys( (Table) itr.next(), done );
		}

	}
	
	protected void secondPassCompileForeignKeys(Table table, Set<ForeignKey> done) throws MappingException {
		table.createForeignKeys();
		Iterator iter = table.getForeignKeyIterator();
		while ( iter.hasNext() ) {

			ForeignKey fk = (ForeignKey) iter.next();
			if ( !done.contains( fk ) ) {
				done.add( fk );
				final String referencedEntityName = fk.getReferencedEntityName();
				if ( referencedEntityName == null ) {
					throw new MappingException(
							"An association from the table " +
							fk.getTable().getName() +
							" does not specify the referenced entity"
						);
				}
				log.debug( "Resolving reference to class: " + referencedEntityName );
				PersistentClass referencedClass = classes.get( referencedEntityName );
				if ( referencedClass == null ) {
					throw new MappingException(
							"An association from the table " +
							fk.getTable().getName() +
							" refers to an unmapped class: " +
							referencedEntityName
						);
				}
				if ( referencedClass.isJoinedSubclass() ) {
					secondPassCompileForeignKeys( referencedClass.getSuperclass().getTable(), done );
				}
				fk.setReferencedTable( referencedClass.getTable() );
				fk.alignColumns();
			}
		}
	}
	
	public Iterator<Table> getTableMappings() {
		return tables.values().iterator();
	}
	
	public PersistentClass getClassMapping(String entityName) {
		return classes.get( entityName );
	}
	
	private void processSecondPassesOfType(Class<? extends SecondPass> type) {
		Iterator iter = secondPasses.iterator();
		while ( iter.hasNext() ) {
			SecondPass sp = ( SecondPass ) iter.next();
			if ( type.isInstance( sp ) ) {
				sp.doSecondPass( classes );
				iter.remove();
			}
		}
	}
	
	//元数据来源，该项目中只来自注解信息
	public static final MetadataSourceType[] DEFAULT_ARTEFACT_PROCESSING_ORDER = new MetadataSourceType[] {
		MetadataSourceType.CLASS
    };

	private List<MetadataSourceType> determineMetadataSourcePrecedence() {
		
		if(metadataSourcePrecedence.isEmpty()
				&& StringHelper.isNotEmpty(getProperties().getProperty(ARTEFACT_PROCESSING_ORDER))){
			metadataSourcePrecedence = parsePrecedence( getProperties().getProperty( ARTEFACT_PROCESSING_ORDER ) );
		}
		if ( metadataSourcePrecedence.isEmpty() ) {
			metadataSourcePrecedence = Arrays.asList( DEFAULT_ARTEFACT_PROCESSING_ORDER );
		}
		metadataSourcePrecedence = Collections.unmodifiableList( metadataSourcePrecedence );
		return metadataSourcePrecedence;
		
	}
	
	private List<MetadataSourceType> parsePrecedence(String s) {
		if ( StringHelper.isEmpty( s ) ) {
			return Collections.emptyList();
		}
		StringTokenizer precedences = new StringTokenizer( s, ",; ", false );
		List<MetadataSourceType> tmpPrecedences = new ArrayList<MetadataSourceType>();
		while ( precedences.hasMoreElements() ) {
			tmpPrecedences.add( MetadataSourceType.parsePrecedence( ( String ) precedences.nextElement() ) );
		}
		return tmpPrecedences;
	}

	
	public Properties getProperties() {
		return properties;
	}
	
	public void buildMappings() {
		secondPassCompile();
	}

	public Mapping buildMapping() {
		return new Mapping() {
			public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
				return identifierGeneratorFactory;
			}

			public Type getIdentifierType(String entityName) throws MappingException {
				PersistentClass pc = classes.get( entityName );
				if ( pc == null ) {
					throw new MappingException( "persistent class not known: " + entityName );
				}
				return pc.getIdentifier().getType();
			}

			public String getIdentifierPropertyName(String entityName) throws MappingException {
				final PersistentClass pc = classes.get( entityName );
				if ( pc == null ) {
					throw new MappingException( "persistent class not known: " + entityName );
				}
				if ( !pc.hasIdentifierProperty() ) {
					return null;
				}
				return pc.getIdentifierProperty().getName();
			}

			public Type getReferencedPropertyType(String entityName, String propertyName) throws MappingException {
				final PersistentClass pc = classes.get( entityName );
				if ( pc == null ) {
					throw new MappingException( "persistent class not known: " + entityName );
				}
				Property prop = pc.getReferencedProperty( propertyName );
				if ( prop == null ) {
					throw new MappingException(
							"property not known: " +
							entityName + '.' + propertyName
						);
				}
				return prop.getType();
			}
		};
	}

	public Configuration(SettingsFactory settingsFactory) {
      this.settingsFactory = settingsFactory;
      reset();
	}
	
	public ReflectionManager getReflectionManager(){
		return reflectionManager;
	}

	protected void reset() {
		metadataSourceQueue = new MetadataSourceQueue();
		createReflectionManager();
		
		classes = new HashMap<String,PersistentClass>();
		tables = new TreeMap<String,Table>();
		secondPasses = new ArrayList<SecondPass>();
		classTypes = new HashMap<String, AnnotatedClassType>();
		namingStrategy = EJB3NamingStrategy.INSTANCE;
		sqlFunctions = new HashMap<String, SQLFunction>();
		propertyReferences = new ArrayList<Mappings.PropertyReference>();
		columnNameBindingPerTable = new HashMap();
		metadataSourcePrecedence = Collections.emptyList();
		joins = new HashMap<String, Map<String,Join>>();
		identifierGeneratorFactory = new DefaultIdentifierGeneratorFactory();
		xmlHelper = new XMLHelper();	
		interceptor = EmptyInterceptor.INSTANCE;
		tableNameBinding = new HashMap();
		mappedByResolver = new HashMap<String, String>();
		typeDefs = new HashMap<String,TypeDef>();
		entityTuplizerFactory = new EntityTuplizerFactory();
		imports = new HashMap<String,String>();
		properties = Environment.getProperties();
		propertyRefResolver = new HashMap<String, String>();
		collections = new HashMap<String,Collection>();
		propertiesAnnotatedWithMapsId = new HashMap<XClass, Map<String, PropertyData>>();
	}
	
	private void createReflectionManager() {
		createReflectionManager(new JPAMetadataProvider());
	}

	private void createReflectionManager(MetadataProvider metadataProvider) {
       	reflectionManager = new JavaReflectionManager();
       	((MetadataProviderInjector)reflectionManager).setMetadataProvider(metadataProvider);
	}

	public Configuration configure() throws SAXException{
		configure("/config.xml");
		return this;
	}

	public Configuration configure(String resource) throws SAXException {
      log.debug("Configuring from resource:"+resource);
      InputStream stream = getConfigurationInputStream( resource );
      return doConfigure(stream,resource);
	}

	protected Configuration doConfigure(InputStream stream, String resourceName) throws SAXException {
        try{
        	XMLErrorLogger xmlErrorLogger = new XMLErrorLogger(resourceName);
        	Document document = xmlHelper.createSAXReader(xmlErrorLogger, null)
        			.read(new InputSource(stream));
        	if(xmlErrorLogger.hasErrors()){
        		throw new MappingException("invalid configuration",xmlErrorLogger.getErrors().get(0));
        	}
        	doConfigure(document);
        }
        catch(DocumentException e){
        	throw new ZormException("Could not parse configuration: "+resourceName,e);
        }
        finally{
        	try{
        		stream.close();
        	}
        	catch(IOException ioe){
        		log.warn("Could not close input stream for "+resourceName);
        	}
        }
		return this;
	}

	protected Configuration doConfigure(Document document) {
		Element sfNode = document.getRootElement().element( "session-factory" );
		String name = sfNode.attributeValue( "name" );
		if ( name != null ) {
			properties.setProperty( Environment.SESSION_FACTORY_NAME, name );
		}
		else{
			properties.setProperty(Environment.SESSION_FACTORY_NAME, "DEFAULT_SESSION_FRACTORY");
		}
		//获取配置文件中的<property>属性的值
		addProperties( sfNode );
		parseSessionFactory( sfNode, name );

		log.debug("Configured SessionFactory: "+name);
		log.info("Properties: "+properties);
		
		return this;
	}

	private void parseSessionFactory(Element sfNode, String name) {
		Iterator elements = sfNode.elementIterator();
		while(elements.hasNext()){
			Element subelement = (Element)elements.next();
			String subelementName = subelement.getName();
			if("mapping".equals(subelementName)){
				parseMappingElement(subelement,name);
			}
		}
	}

	private void parseMappingElement(Element mappingElement, String name) {
		final Attribute classAttribute = mappingElement.attribute( "class" );
		if(classAttribute != null){
			final String className = classAttribute.getValue();
			log.debug("Session-factory config ["+name+"] named class ["+className+"] for mapping");
			try{
				addAnnotatedClass(ReflectHelper.classForName(className));
			}
			catch(Exception e){
				throw new MappingException("Unable to load class [ " + className + "] "
						+ "declared in configuration <mapping/> entry",e);
			}
		}
		else
		{
			throw new MappingException( "<mapping> element in configuration specifies no known attributes" );
		}
	}

	@SuppressWarnings("unchecked")
	private Configuration addAnnotatedClass(Class annotatedClass) {
		XClass xClass = reflectionManager.toXClass(annotatedClass);
		//将标注类添加到待处理队列中
		metadataSourceQueue.add(xClass);
		return this;
	}

	@SuppressWarnings("rawtypes")
	private void addProperties(Element parent) {
		Iterator iterator = parent.elementIterator("property");
		while(iterator.hasNext()){
			Element node = (Element)iterator.next();
			String name = node.attributeValue("name");
			String value = node.getText().trim();
			log.info(name+"="+value);
			properties.setProperty(name, value);
			//另外添加属性，方便以后获取属性
			if(!name.startsWith("zorm")){
				properties.setProperty("zorm."+name, value);
			}
		}
		Environment.verifyProperties(properties);
	}

	protected InputStream getConfigurationInputStream(String resource) {
        log.info("Configuring resource:"+resource);
		return ConfigHelper.getResourceAsStream(resource);
	}
	
	@SuppressWarnings("serial")
	protected class MappingsImpl implements Mappings, Serializable {

		private String schemaName;
		private String defaultPackage;
		private String catalogName;
		private boolean autoImport;
		private boolean defaultLazy;
		private String defaultCascade;
		private String defaultAccess;

		public String getSchemaName() {
			return schemaName;
		}

		public void setSchemaName(String schemaName) {
			this.schemaName = schemaName;
		}

		public String getCatalogName() {
			return catalogName;
		}
		
		public Properties getConfigurationProperties() {
			return properties;
		}

		public void setCatalogName(String catalogName) {
			this.catalogName = catalogName;
		}

		public String getDefaultPackage() {
			return defaultPackage;
		}

		public void setDefaultPackage(String defaultPackage) {
			this.defaultPackage = defaultPackage;
		}

		public boolean isAutoImport() {
			return autoImport;
		}

		public void setAutoImport(boolean autoImport) {
			this.autoImport = autoImport;
		}

		public boolean isDefaultLazy() {
			return defaultLazy;
		}

		public void setDefaultLazy(boolean defaultLazy) {
			this.defaultLazy = defaultLazy;
		}

		public String getDefaultCascade() {
			return defaultCascade;
		}

		public void setDefaultCascade(String defaultCascade) {
			this.defaultCascade = defaultCascade;
		}

		public String getDefaultAccess() {
			return defaultAccess;
		}

		public void setDefaultAccess(String defaultAccess) {
			this.defaultAccess = defaultAccess;
		}

		public NamingStrategy getNamingStrategy() {
			return namingStrategy;
		}

		public void setNamingStrategy(NamingStrategy namingStrategy) {
			Configuration.this.namingStrategy = namingStrategy;
		}

		public TypeResolver getTypeResolver() {
			return typeResolver;
		}

		public Map<String, Join> getJoins(String entityName) {
			return joins.get( entityName );
		}
		
		public Iterator<PersistentClass> iterateClasses() {
			return classes.values().iterator();
		}
		
		public String getPropertyReferencedAssociation(String entityName, String propertyName) {
			return propertyRefResolver.get( entityName + "." + propertyName );
		}

		public PersistentClass getClass(String entityName) {
			return classes.get( entityName );
		}
		
		public String getFromMappedBy(String entityName, String propertyName) {
			return mappedByResolver.get( entityName + "." + propertyName );
		}
		
		public void addUniquePropertyReference(String referencedClass, String propertyName) {
			propertyReferences.add( new PropertyReference( referencedClass, propertyName, true ) );
		}
		
		public void addPropertyReference(String referencedClass, String propertyName) {
			propertyReferences.add( new PropertyReference( referencedClass, propertyName, false ) );
		}
		
		public void addMappedBy(String entityName, String propertyName, String inversePropertyName) {
			mappedByResolver.put( entityName + "." + propertyName, inversePropertyName );
		}

		public PropertyData getPropertyAnnotatedWithMapsId(XClass entityType,
				String propertyName){
			final Map<String,PropertyData> map = propertiesAnnotatedWithMapsId.get(entityType);
			return map == null ? null : map.get(propertyName);
		}
		
		public PersistentClass locatePersistentClassByEntityName(String entityName) {
			PersistentClass persistentClass = classes.get( entityName );
			if ( persistentClass == null ) {
				String actualEntityName = imports.get( entityName );
				if ( StringHelper.isNotEmpty( actualEntityName ) ) {
					persistentClass = classes.get( actualEntityName );
				}
			}
			return persistentClass;
		}

		public void addClass(PersistentClass persistentClass) throws DuplicateMappingException {
			Object old = classes.put( persistentClass.getEntityName(), persistentClass );
			if ( old != null ) {
				throw new DuplicateMappingException( "class/entity", persistentClass.getEntityName() );
			}
		}

		public void addImport(String entityName, String rename) throws DuplicateMappingException {
			String existing = imports.put( rename, entityName );
			if ( existing != null ) {
                if (existing.equals(entityName)) 
                	log.info("Duplicate import:"+entityName+"=>"+rename);
                else throw new DuplicateMappingException("duplicate import: " + rename + " refers to both " + entityName + " and "
                                                         + existing + " (try using auto-import=\"false\")", "import", rename);
			}
		}

		public boolean isInSecondPass() {
			return inSecondPass;
		}
		
		public Table getTable(String schema, String catalog, String name) {
			String key = Table.qualify(catalog, schema, name);
			return tables.get(key);
		}

		public Iterator<Table> iterateTables() {
			return tables.values().iterator();
		}

		public Table addTable(
				String schema,
				String catalog,
				String name,
				String subselect,
				Boolean isAbstract) {
			name = getObjectNameNormalizer().normalizeIdentifierQuoting( name );
			schema = getObjectNameNormalizer().normalizeIdentifierQuoting( schema );
			catalog = getObjectNameNormalizer().normalizeIdentifierQuoting( catalog );

			//key: schema.catalog.name
			String key = subselect == null ? Table.qualify( catalog, schema, name ) : subselect;
			Table table = tables.get( key );

			if ( table == null ) {
				table = new Table();
				table.setAbstract( isAbstract );
				//name是表名
				table.setName( name );
				table.setSchema( schema );
				table.setCatalog( catalog );
				table.setSubselect( subselect );
				tables.put( key, table );
			}
			else {
				if ( !isAbstract ) {
					table.setAbstract( false );
				}
			}

			return table;
		}


		public ResultSetMappingDefinition getResultSetMapping(String name) {
			return sqlResultSetMappings.get(name);
		}


		public void applyResultSetMapping(ResultSetMappingDefinition sqlResultSetMapping) throws DuplicateMappingException {
			Object old = sqlResultSetMappings.put( sqlResultSetMapping.getName(), sqlResultSetMapping );
			if ( old != null ) {
				throw new DuplicateMappingException( "resultSet",  sqlResultSetMapping.getName() );
			}
		}


		protected void removeResultSetMapping(String name) {
			sqlResultSetMappings.remove( name );
		}

		private class TableDescription implements Serializable {
			final String logicalName;
			final Table denormalizedSupertable;

			TableDescription(String logicalName, Table denormalizedSupertable) {
				this.logicalName = logicalName;
				this.denormalizedSupertable = denormalizedSupertable;
			}
		}


		private String buildTableNameKey(String schema, String catalog, String finalName) {
			StringBuilder keyBuilder = new StringBuilder();
			if (schema != null) keyBuilder.append( schema );
			keyBuilder.append( ".");
			if (catalog != null) keyBuilder.append( catalog );
			keyBuilder.append( ".");
			keyBuilder.append( finalName );
			return keyBuilder.toString();
		}
		
		public boolean isSpecjProprietarySyntaxEnabled() {
			return specjProprietarySyntaxEnabled;
		}
		
		public void addPropertyAnnotatedWithMapsIdSpecj(XClass entityType, PropertyData property, String mapsIdValue) {
			Map<String, PropertyData> map = propertiesAnnotatedWithMapsId.get( entityType );
			if ( map == null ) {
				map = new HashMap<String, PropertyData>();
				propertiesAnnotatedWithMapsId.put( entityType, map );
			}
			map.put( mapsIdValue, property );
		}

		@Override
		public AnnotatedClassType getClassType(XClass clazz) {
			AnnotatedClassType type = classTypes.get(clazz);
			if(type == null){
				return addClassType(clazz);
			}
			else{
			  return type;
			}
		}
		
		public AnnotatedClassType addClassType(XClass clazz) {
			AnnotatedClassType type;
			if ( clazz.isAnnotationPresent( Entity.class ) ) {
				type = AnnotatedClassType.ENTITY;
			}
			else if ( clazz.isAnnotationPresent( Embeddable.class ) ) {
				type = AnnotatedClassType.EMBEDDABLE;
			}
			else if ( clazz.isAnnotationPresent( javax.persistence.MappedSuperclass.class ) ) {
				type = AnnotatedClassType.EMBEDDABLE_SUPERCLASS;
			}
			else {
				type = AnnotatedClassType.NONE;
			}
			classTypes.put( clazz.getName(), type );
			return type;
		}
		
		final ObjectNameNormalizer normalizer = new ObjectNameNormalizerImpl();
		
		@Override
		public ObjectNameNormalizer getObjectNameNormalizer() {
			return normalizer;
		}
		
		final class ObjectNameNormalizerImpl extends ObjectNameNormalizer implements Serializable {
			public boolean isUseQuotedIdentifiersGlobally() {
				String setting = (String) properties.get( Environment.GLOBALLY_QUOTED_IDENTIFIERS );
				return setting != null && Boolean.valueOf( setting ).booleanValue();
			}

			public NamingStrategy getNamingStrategy() {
				return namingStrategy;
			}
		}

		@Override
		public Table addDenormalizedTable(
				String schema, 
				String catalog,
				String name, 
				Boolean isAbstract, 
				String subselect,
				Table includedTable)
				throws DuplicateMappingException {
			name = getObjectNameNormalizer().normalizeIdentifierQuoting( name );
			schema = getObjectNameNormalizer().normalizeIdentifierQuoting( schema );
			catalog = getObjectNameNormalizer().normalizeIdentifierQuoting( catalog );

			String key = subselect == null ? Table.qualify(catalog, schema, name) : subselect;
			if ( tables.containsKey( key ) ) {
				throw new DuplicateMappingException( "table", name );
			}

			Table table = new DenormalizedTable( includedTable );
			table.setAbstract( isAbstract );
			table.setName( name );
			table.setSchema( schema );
			table.setCatalog( catalog );
			table.setSubselect( subselect );

			tables.put( key, table );
			return table;
		}

		@Override
		public void addUniqueConstraintHolders(Table table,
				List<UniqueConstraintHolder> uniqueConstraints) {
		}

		@Override
		public void addTableBinding(String schema, String catalog,
				String logicalName, String physicalName,
				Table denormalizedSuperTable) throws DuplicateMappingException {
			String key = buildTableNameKey( schema, catalog, physicalName );
			TableDescription tableDescription = new TableDescription( logicalName, denormalizedSuperTable );
			TableDescription oldDescriptor = ( TableDescription ) tableNameBinding.put( key, tableDescription );
			if ( oldDescriptor != null && ! oldDescriptor.logicalName.equals( logicalName ) ) {
				throw new DuplicateMappingException(
						"Same physical table name [" + physicalName + "] references several logical table names: [" +
								oldDescriptor.logicalName + "], [" + logicalName + ']',
						"table",
						physicalName
				);
			}
		}

		@Override
		public ReflectionManager getReflectionManager() {
			return reflectionManager;
		}

		@Override
		public boolean useNewGeneratorMappings() {
			return false;
		}

		private Boolean forceDiscriminatorInSelectsByDefault;
		
		@Override
		public boolean forceDiscriminatorInSelectsByDefault() {
			if ( forceDiscriminatorInSelectsByDefault == null ) {
				final String booleanName = getConfigurationProperties()
						.getProperty( AvailableSettings.FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT );
				forceDiscriminatorInSelectsByDefault = Boolean.valueOf( booleanName );
			}
			return forceDiscriminatorInSelectsByDefault.booleanValue();
		}

		@Override
		public void addToOneAndIdProperty(XClass entity,
				PropertyData propertyAnnotatedElement) {
			
		}

		@Override
		public void addPropertyAnnotatedWithMapsId(XClass entity,
				PropertyData propertyAnnotatedElement) {
			
		}

		@Override
		public MappedSuperclass getMappedSuperclass(Class<?> type) {
			return null;
		}

		@Override
		public void addMappedSuperclass(Class<?> type,
				MappedSuperclass mappedSuperclass) {
			
		}

		@Override
		public PropertyData getPropertyAnnotatedWithIdAndToOne(
				XClass persistentXClass, String propertyName) {
			return null;
		}

		@Override
		public void addColumnBinding(String logicalName,
				Column physicalColumn, Table table)
				throws DuplicateMappingException {
			TableColumnNameBinding binding = ( TableColumnNameBinding ) columnNameBindingPerTable.get( table );
			if(binding==null){
				binding = new TableColumnNameBinding(table.getName());
				columnNameBindingPerTable.put(table, binding);
			}
			binding.addBinding(logicalName,physicalColumn);
		}
		
		private class TableColumnNameBinding implements Serializable{
			private final String tableName;
			private Map logicalToPhysical = new HashMap();
			private Map physicalToLogical = new HashMap();
			
			private TableColumnNameBinding(String tableName) {
				this.tableName = tableName;
			}

			public void addBinding(String logicalName, Column physicalColumn) {
				bindLogicalToPhysical(logicalName,physicalColumn);
				bindPhysicalToLogical( logicalName, physicalColumn );
			}

			private void bindPhysicalToLogical(String logicalName,
					Column physicalColumn) {
				final String physicalName = physicalColumn.getQuotedName();
				final String existingLogicalName = ( String ) physicalToLogical.put( physicalName, logicalName );
				if ( existingLogicalName != null && ! existingLogicalName.equals( logicalName ) ) {
					throw new DuplicateMappingException(
							" Table [" + tableName + "] contains phyical column name [" + physicalName
									+ "] represented by different logical column names: [" + existingLogicalName
									+ "], [" + logicalName + "]",
							"column-binding",
							tableName + "." + physicalName
					);
				}
			}
			
			private void bindLogicalToPhysical(String logicalName,
					Column physicalColumn) {
				final String logicalKey = logicalName.toLowerCase();
				final String physicalName = physicalColumn.getQuotedName();
				final String existingPhysicalName = ( String ) logicalToPhysical.put( logicalKey, physicalName );
				if ( existingPhysicalName != null ) {
					boolean areSamePhysicalColumn = physicalColumn.isQuoted()
							? existingPhysicalName.equals( physicalName )
							: existingPhysicalName.equalsIgnoreCase( physicalName );
					if ( ! areSamePhysicalColumn ) {
						throw new DuplicateMappingException(
								" Table [" + tableName + "] contains logical column name [" + logicalName
										+ "] referenced by multiple physical column names: [" + existingPhysicalName
										+ "], [" + physicalName + "]",
								"column-binding",
								tableName + "." + logicalName
						);
					}
				}
			}
		}


		@Override
		public void addSecondPass(SecondPass secondPass) {
			addSecondPass(secondPass,false);
		}

		public void addSecondPass(SecondPass secondPass, boolean b) {
			if(b){
				secondPasses.add(0,secondPass);
			}
			else{
				secondPasses.add(secondPass);
			}
		}
		
		public void addCollection(Collection collection) throws DuplicateMappingException {
			Object old = collections.put( collection.getRole(), collection );
			if ( old != null ) {
				throw new DuplicateMappingException( "collection role", collection.getRole() );
			}
		}

		@Override
		public TypeDef getTypeDef(String typeName) {
			return typeDefs.get(typeName);
		}

		public String getLogicalColumnName(String physicalName, Table table) throws MappingException {
			String logical = null;
			Table currentTable = table;
			TableDescription description = null;
			do {
				TableColumnNameBinding binding = ( TableColumnNameBinding ) columnNameBindingPerTable.get( currentTable );
				if ( binding != null ) {
					logical = ( String ) binding.physicalToLogical.get( physicalName );
				}
				String key = buildTableNameKey(
						currentTable.getQuotedSchema(), currentTable.getQuotedCatalog(), currentTable.getQuotedName()
				);
				description = ( TableDescription ) tableNameBinding.get( key );
				if ( description != null ) {
					currentTable = description.denormalizedSupertable;
				}
				else {
					currentTable = null;
				}
			}
			while ( logical == null && currentTable != null && description != null );
			if ( logical == null ) {
				throw new MappingException(
						"Unable to find logical column name from physical name "
								+ physicalName + " in table " + table.getName()
				);
			}
			return logical;
		}

		@Override
		public String getPhysicalColumnName(String logicalName, Table table) {
			logicalName = logicalName.toLowerCase();
			String finalName = null;
			Table currentTable = table;
			do {
				TableColumnNameBinding binding = ( TableColumnNameBinding ) columnNameBindingPerTable.get( currentTable );
				if ( binding != null ) {
					finalName = ( String ) binding.logicalToPhysical.get( logicalName );
				}
				String key = buildTableNameKey(
						currentTable.getQuotedSchema(), currentTable.getQuotedCatalog(), currentTable.getQuotedName()
				);
				TableDescription description = ( TableDescription ) tableNameBinding.get( key );
				if ( description != null ) {
					currentTable = description.denormalizedSupertable;
				}
				else {
					currentTable = null;
				}
			} while ( finalName == null && currentTable != null );

			if ( finalName == null ) {
				throw new MappingException(
						"Unable to find column with logical name " + logicalName + " in table " + table.getName()
				);
			}
			return finalName;
		}

		public String getLogicalTableName(Table table) throws MappingException {
			return getLogicalTableName( table.getQuotedSchema(), table.getQuotedCatalog(), table.getQuotedName() );
		}
		
		private String getLogicalTableName(String schema, String catalog, String physicalName) throws MappingException {
			String key = buildTableNameKey( schema, catalog, physicalName );
			TableDescription descriptor = (TableDescription) tableNameBinding.get( key );
			if (descriptor == null) {
				throw new MappingException( "Unable to find physical table: " + physicalName);
			}
			return descriptor.logicalName;
		}

		@Override
		public void addJoins(PersistentClass persistentClass,
				Map<String, Join> joins) {
			Object old = Configuration.this.joins.put( persistentClass.getEntityName(), joins );
			if(old!=null){
				log.info("duplicate Joins");
			}
		}

		@Override
		public Map getClasses() {
			return classes;
		}

    }

	@SuppressWarnings("serial")
	protected class MetadataSourceQueue implements Serializable{
		private transient List<XClass> annotatedClasses = new ArrayList<XClass>();
		private transient Map<String, XClass> annotatedClassesByEntityNameMap = new HashMap<String, XClass>();
	
		private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
			ois.defaultReadObject();
			annotatedClassesByEntityNameMap = new HashMap<String, XClass>();

			List<Class> serializableAnnotatedClasses = (List<Class>) ois.readObject();
			annotatedClasses = new ArrayList<XClass>( serializableAnnotatedClasses.size() );
			for ( Class clazz : serializableAnnotatedClasses ) {
				annotatedClasses.add( reflectionManager.toXClass( clazz ) );
			}
		}		
		
		private void writeObject(ObjectOutputStream out) throws IOException {
			out.defaultWriteObject();
			List<Class> serializableAnnotatedClasses = new ArrayList<Class>( annotatedClasses.size() );
			for ( XClass xClass : annotatedClasses ) {
				serializableAnnotatedClasses.add( reflectionManager.toClass( xClass ) );
			}
			out.writeObject( serializableAnnotatedClasses );
		}		
		
		public void add(XClass annotatedClass){
			annotatedClasses.add(annotatedClass);
		}
		
		protected void syncAnnotatedClasses(){
			final Iterator<XClass> itr = annotatedClasses.iterator();
			while(itr.hasNext()){
				final XClass annotatedClass = itr.next();
				if(annotatedClass.isAnnotationPresent(Entity.class)){
					annotatedClassesByEntityNameMap.put(annotatedClass.getName(), annotatedClass);
				    continue;
				}
				if ( !annotatedClass.isAnnotationPresent( javax.persistence.MappedSuperclass.class ) ) {
					itr.remove();
				}
			}
		}
		
		protected void processMetadata(List<MetadataSourceType> order){
			syncAnnotatedClasses();
			
			for(MetadataSourceType type : order){
				if(MetadataSourceType.CLASS.equals(type)){
					processAnnotatedClassesQueue();
				}
			}
		}

		private void processAnnotatedClassesQueue() {
			log.info("Process annotated classes");
			
			List<XClass> orderedClasses = orderAndFillHierarchy( annotatedClasses );
			Mappings mappings = createMappings();
			
			//若没有父类，InheritanceType为SINGLE_TABLE
			//获取实体类的继承信息，包括父类的继承信息
			Map<XClass, InheritanceState> inheritanceStatePerClass = AnnotationBinder.buildInheritanceStates(
					orderedClasses, mappings
			);


			for ( XClass clazz : orderedClasses ) {
				AnnotationBinder.bindClass( clazz, inheritanceStatePerClass, mappings );
			}
			annotatedClasses.clear();
			annotatedClassesByEntityNameMap.clear();
		}

		private Mappings createMappings() {
			return new MappingsImpl();
		}

		private List<XClass> orderAndFillHierarchy(List<XClass> original) {
			List<XClass> copy = new ArrayList<XClass>( original );
			insertMappedSuperclasses( original, copy );

			// order the hierarchy
			List<XClass> workingCopy = new ArrayList<XClass>( copy );
			List<XClass> newList = new ArrayList<XClass>( copy.size() );
			while ( workingCopy.size() > 0 ) {
				XClass clazz = workingCopy.get( 0 );
				orderHierarchy( workingCopy, newList, copy, clazz );
			}
			return newList;
		}

		private void insertMappedSuperclasses(List<XClass> original, List<XClass> copy) {
			for ( XClass clazz : original ) {
				XClass superClass = clazz.getSuperclass();
				while ( superClass != null
						&& !reflectionManager.equals( superClass, Object.class )
						&& !copy.contains( superClass ) ) {
					if ( superClass.isAnnotationPresent( Entity.class )
							|| superClass.isAnnotationPresent( javax.persistence.MappedSuperclass.class ) ) {
						copy.add( superClass );
					}
					superClass = superClass.getSuperclass();
				}
			}
		}

		private void orderHierarchy(List<XClass> copy, List<XClass> newList, List<XClass> original, XClass clazz) {
			if ( clazz == null || reflectionManager.equals( clazz, Object.class ) ) {
				return;
			}
			//process superclass first
			orderHierarchy( copy, newList, original, clazz.getSuperclass() );
			if ( original.contains( clazz ) ) {
				if ( !newList.contains( clazz ) ) {
					newList.add( clazz );
				}
				copy.remove( clazz );
			}
		}

		public boolean isEmpty() {
			return annotatedClasses.isEmpty();
		}
	}

	public Interceptor getInterceptor() {
		return interceptor;
	}

	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return entityNotFoundDelegate;
	}

	public Map getSqlFunctions() {
		return sqlFunctions;
	}

	public TypeResolver getTypeResolver() {
		return typeResolver;
	}

	public Iterator<PersistentClass> getClassMappings() {
		return classes.values().iterator();
	}

	public MutableIdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return identifierGeneratorFactory;
	}

	public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
		return currentTenantIdentifierResolver;
	}

	public Map<String,String> getImports() {
		return imports;
	}

	public Iterator getCollectionMappings() {
		return collections.values().iterator();
	}
}
