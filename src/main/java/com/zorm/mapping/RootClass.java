package com.zorm.mapping;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.util.SingletonIterator;

@SuppressWarnings("unused")
public class RootClass extends PersistentClass implements TableOwner {

	private static final long serialVersionUID = -3534112800371051501L;
	private static final Log log = LogFactory.getLog(RootClass.class);
	
	public static final String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";
	public static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "class";

	private Property identifierProperty; 
	private KeyValue identifier; 
	private Property version; 
	private boolean polymorphic;
	private Value discriminator; 
	private boolean mutable = true;
	private boolean embeddedIdentifier = false; 
	private boolean explicitPolymorphism;
	private Class entityPersisterClass;
	private boolean forceDiscriminator = false;
	private String where;
	private Table table;
	private boolean discriminatorInsertable = true;
	private int nextSubclassId = 0;
	private Property declaredIdentifierProperty;
	private Property declaredVersion;


	public void setTable(Table table) {
		this.table=table;
	}
	@Override
    public Table getTable() {
		return table;
	}
	
	@Override
    public Iterator getKeyClosureIterator() {
		return new SingletonIterator( getKey() );
	}

	@Override
    public KeyValue getKey() {
		return getIdentifier();
	}
	
	@Override
    public Property getIdentifierProperty() {
		return identifierProperty;
	}
	
	@Override
	public void addSubClass(Subclass subclass) {
		super.addSubClass(subclass);
		setPolymorphic(true);
	}

	public void setDeclaredIdentifierProperty(Property declaredIdentifierProperty) {
		this.declaredIdentifierProperty = declaredIdentifierProperty;
	}

	@Override
    public KeyValue getIdentifier() {
		return identifier;
	}
	
	@Override
    public boolean hasIdentifierProperty() {
		return identifierProperty!=null;
	}
	
	@Override
    public Iterator getTableClosureIterator() {
		return new SingletonIterator( getTable() );
	}
	
	@Override
	public int getSubclassId() {
		return 0;
	}

	@Override
    public Value getDiscriminator() {
		return discriminator;
	}
	
	@Override
	public boolean isForceDiscriminator() {
		return forceDiscriminator;
	}

	@Override
    public boolean isInherited() {
		return false;
	}
	

	public void setPolymorphic(boolean polymorphic) {
		this.polymorphic = polymorphic;
	}


	@Override
    public Iterator getPropertyClosureIterator() {
		return getPropertyIterator();
	}

	@Override
    public Property getVersion() {
		return version;
	}

	public void setDeclaredVersion(Property declaredVersion) {
		this.declaredVersion = declaredVersion;
	}

	public void setVersion(Property version) {
		this.version = version;
	}

	public void setDiscriminator(Value discriminator) {
		this.discriminator = discriminator;
	}

	public void setEmbeddedIdentifier(boolean embeddedIdentifier) {
		this.embeddedIdentifier = embeddedIdentifier;
	}

	public void setExplicitPolymorphism(boolean explicitPolymorphism) {
		this.explicitPolymorphism = explicitPolymorphism;
	}

	public void setIdentifier(KeyValue identifier) {
		this.identifier = identifier;
	}

	public void setIdentifierProperty(Property identifierProperty) {
		this.identifierProperty = identifierProperty;
		identifierProperty.setPersistentClass(this);

	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}


	public void setDiscriminatorInsertable(boolean insertable) {
		this.discriminatorInsertable = insertable;
	}


	public void setForceDiscriminator(boolean forceDiscriminator) {
		this.forceDiscriminator = forceDiscriminator;
	}


	public void setWhere(String string) {
		where = string;
	}


	@Override
	public void addFilter(String name, String condition,
			boolean autoAliasInjection, Map<String, String> aliasTableMap,
			Map<String, String> aliasEntityMap) {
		
	}


	@Override
	public MetaAttribute getMetaAttribute(String name) {
		return null;
	}
	
	@Override
	public boolean isPolymorphic() {
		return polymorphic;
	}
	
	@Override
	public Table getRootTable(){
		return getTable();
	}
	
	public Set getIdentityTables() {
		Set tables = new HashSet();
		Iterator iter = getSubclassClosureIterator();
		while(iter.hasNext()){
			PersistentClass clazz = (PersistentClass)iter.next();
			if(clazz.isAbstract()==null || !clazz.isAbstract().booleanValue())
				tables.add(clazz.getIdentityTable());
		}
		return tables;
	}
	
	@Override
	int nextSubclassId() {
		return ++nextSubclassId;
	}
	@Override
	public RootClass getRootClass() {
		return this;
	}
	@Override
	public String getWhere() {
		return where;
	}
	@Override
	public boolean isVersioned() {
		return version!=null;
	}
	@Override
	public boolean hasEmbeddedIdentifier() {
		return embeddedIdentifier;
	}
	@Override
	public boolean isMutable() {
		return mutable;
	}
	@Override
	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}
	@Override
	public PersistentClass getSuperclass() {
		return null;
	}
	@Override
	public int getOptimisticLockMode() {
		return optimisticLockMode;
	}
	@Override
	public Class getEntityPersisterClass() {
		return entityPersisterClass;
	}
	@Override
	public boolean isDiscriminatorInsertable() {
		return discriminatorInsertable;
	}
	
	@Override
    public boolean isJoinedSubclass() {
		return false;
	}

}
