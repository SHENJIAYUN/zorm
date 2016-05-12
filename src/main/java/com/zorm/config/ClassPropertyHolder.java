package com.zorm.config;

import java.util.Map;

import javax.persistence.Column;

import com.zorm.annotations.reflection.XClass;
import com.zorm.config.annotations.EntityBinder;
import com.zorm.exception.AssertionFailure;
import com.zorm.mapping.Join;
import com.zorm.mapping.KeyValue;
import com.zorm.mapping.MappedSuperclass;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Property;
import com.zorm.mapping.Table;

public class ClassPropertyHolder extends AbstractPropertyHolder {

	private Map<String, Join> joins;
	private PersistentClass persistentClass;
	private EntityBinder entityBinder;
	private final Map<XClass, InheritanceState> inheritanceStatePerClass;

	public ClassPropertyHolder(
			PersistentClass persistentClass,
			XClass clazzToProcess,
			Map<String, Join> joins,
			Mappings mappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		super( persistentClass.getEntityName(), null, clazzToProcess, mappings );
		this.persistentClass = persistentClass;
		this.joins = joins;
		this.inheritanceStatePerClass = inheritanceStatePerClass;
	}
	
	public ClassPropertyHolder(
			PersistentClass persistentClass,
			XClass clazzToProcess,
			EntityBinder entityBinder,
			Mappings mappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		this( persistentClass, clazzToProcess, mappings, inheritanceStatePerClass );
		this.entityBinder = entityBinder;
	}
	
	public ClassPropertyHolder(
			PersistentClass persistentClass,
			XClass clazzToProcess,
			Mappings mappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		super( persistentClass.getEntityName(), null, clazzToProcess, mappings );
		this.persistentClass = persistentClass;
		//this.joins = joins;
		this.inheritanceStatePerClass = inheritanceStatePerClass;
	}

	public String getEntityName() {
		return persistentClass.getEntityName();
	}
	
	public String getClassName() {
		return persistentClass.getClassName();
	}

	public String getEntityOwnerClassName() {
		return getClassName();
	}

	public Table getTable() {
		return persistentClass.getTable();
	}

	public boolean isComponent() {
		return false;
	}

	public boolean isEntity() {
		return true;
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public KeyValue getIdentifier() {
		return persistentClass.getIdentifier();
	}

	public boolean isOrWithinEmbeddedId() {
		return false;
	}
	
	private void addPropertyToPersistentClass(Property prop, XClass declaringClass) {
		if ( declaringClass != null ) {
			final InheritanceState inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState == null ) {
				throw new AssertionFailure(
						"Declaring class is not found in the inheritance state hierarchy: " + declaringClass
				);
			}
			if ( inheritanceState.isEmbeddableSuperclass() ) {
				persistentClass.addMappedsuperclassProperty(prop);
				addPropertyToMappedSuperclass( prop, declaringClass );
			}
			else {
				persistentClass.addProperty( prop );
			}
		}
		else {
			persistentClass.addProperty( prop );
		}
	}

	private void addPropertyToMappedSuperclass(Property prop, XClass declaringClass) {
		final Mappings mappings = getMappings();
		final Class type = mappings.getReflectionManager().toClass( declaringClass );
		MappedSuperclass superclass = mappings.getMappedSuperclass( type );
		superclass.addDeclaredProperty( prop );
	}
	
	@Override
	public void addProperty(Property prop, XClass declaringClass) {
//		if ( prop.getValue() instanceof Component ) {
//			//TODO handle quote and non quote table comparison
//			String tableName = prop.getValue().getTable().getName();
//			if ( getJoinsPerRealTableName().containsKey( tableName ) ) {
//				final Join join = getJoinsPerRealTableName().get( tableName );
//				addPropertyToJoin( prop, declaringClass, join );
//			}
//			else {
//				addPropertyToPersistentClass( prop, declaringClass );
//			}
//		}
//		else {
			addPropertyToPersistentClass( prop, declaringClass );
//		}
	}

	@Override
	public void addProperty(Property prop, Ejb3Column[] columns,
			XClass declaringClass) {
		if ( columns != null && columns[0].isSecondary() ) {
			//TODO move the getJoin() code here?
			//final Join join = columns[0].getJoin();
			//addPropertyToJoin( prop, declaringClass, join );
		}
		else {
			addProperty( prop, declaringClass );
		}
	}
}
