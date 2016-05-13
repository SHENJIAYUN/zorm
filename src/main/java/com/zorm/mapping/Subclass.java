package com.zorm.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zorm.util.JoinedIterator;
import com.zorm.util.SingletonIterator;

@SuppressWarnings("unused")
public class Subclass extends PersistentClass{

	private static final long serialVersionUID = 2716807833871599581L;
	
	private PersistentClass superclass;
	private Class classPersisterClass;
	private final int subclassId;
	
	public Subclass(PersistentClass superclass) {
		this.superclass = superclass;
		this.subclassId = superclass.nextSubclassId();
	}
	
	protected void addSubclassTable(Table table) {
		super.addSubclassTable(table);
		getSuperclass().addSubclassTable(table);
	}
	
	public void addProperty(Property p) {
		super.addProperty(p);
		getSuperclass().addSubclassProperty(p);
	}

	public Property getVersion() {
		return getSuperclass().getVersion();
	}
	
	@Override
	public int getSubclassId() {
		return subclassId;
	}
	
	public Iterator getTableClosureIterator() {
		return new JoinedIterator(
				getSuperclass().getTableClosureIterator(),
				new SingletonIterator( getTable() )
			);
	}
	
	public Iterator getKeyClosureIterator() {
		return new JoinedIterator(
				getSuperclass().getKeyClosureIterator(),
				new SingletonIterator( getKey() )
			);
	}
	
	public Iterator getJoinClosureIterator() {
		return new JoinedIterator(
			getSuperclass().getJoinClosureIterator(),
			super.getJoinClosureIterator()
		);
	}
	
	public int getJoinClosureSpan() {
		return getSuperclass().getJoinClosureSpan() + super.getJoinClosureSpan();
	}
	
	public KeyValue getKey() {
		return getSuperclass().getIdentifier();
	}
	
	public boolean isClassOrSuperclassTable(Table table) {
		return super.isClassOrSuperclassTable(table) || getSuperclass().isClassOrSuperclassTable(table);
	}
	
	@Override
	public void addFilter(String name, String condition,
			boolean autoAliasInjection, Map<String, String> aliasTableMap,
			Map<String, String> aliasEntityMap) {
	}

	@Override
	public List getFilters() {
		java.util.List filters = new ArrayList(super.getFilters());
		filters.addAll(getSuperclass().getFilters());
		return filters;
	}
	
	@Override
	public boolean isPolymorphic() {
		return true;
	}


	@Override
	public MetaAttribute getMetaAttribute(String name) {
		return null;
	}

	@Override
	public Value getDiscriminator() {
		return getSuperclass().getDiscriminator();
	}

	@Override
	public boolean isForceDiscriminator() {
		return getSuperclass().isForceDiscriminator();
	}
	
	@Override
	public Table getTable() {
		return getSuperclass().getTable();
	}

	@Override
	public KeyValue getIdentifier() {
		return getSuperclass().getIdentifier();
	}

	@Override
	public boolean hasIdentifierProperty() {
		return false;
	}

	@Override
	public Property getIdentifierProperty() {
		return getSuperclass().getIdentifierProperty();
	}

	@Override
	public Iterator getPropertyClosureIterator() {
		return new JoinedIterator(
				getSuperclass().getPropertyClosureIterator(),
				getPropertyIterator()
			);
	}

	@Override
	public boolean isInherited() {
		return true;
	}

	public int getPropertyClosureSpan() {
		return getSuperclass().getPropertyClosureSpan() + super.getPropertyClosureSpan();
	}

	@Override
	public int nextSubclassId() {
		return getSuperclass().nextSubclassId();
	}


	@Override
	public Table getRootTable() {
		return getSuperclass().getRootTable();
	}


	@Override
	public RootClass getRootClass() {
		return getSuperclass().getRootClass();
	}


	@Override
	public String getWhere() {
		return null;
	}


	@Override
	public boolean isVersioned() {
		return getSuperclass().isVersioned();
	}


	@Override
	public boolean hasEmbeddedIdentifier() {
		return  getSuperclass().hasEmbeddedIdentifier();
	}


	@Override
	public boolean isMutable() {
		return getSuperclass().isMutable();
	}

	public boolean isJoinedSubclass() {
		return getTable()!=getRootTable();
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return false;
	}


	@Override
	public PersistentClass getSuperclass() {
		return superclass;
	}


	@Override
	public int getOptimisticLockMode() {
		return 0;
	}


	@Override
	public Class getEntityPersisterClass() {
		return null;
	}


	@Override
	public boolean isDiscriminatorInsertable() {
		return getSuperclass().isDiscriminatorInsertable();
	}

}
