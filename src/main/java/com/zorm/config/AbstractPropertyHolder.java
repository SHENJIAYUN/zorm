package com.zorm.config;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MappedSuperclass;

import com.zorm.annotations.reflection.XAnnotatedElement;
import com.zorm.annotations.reflection.XClass;
import com.zorm.annotations.reflection.XProperty;
import com.zorm.exception.AssertionFailure;
import com.zorm.util.StringHelper;

public abstract class AbstractPropertyHolder implements PropertyHolder{
	protected AbstractPropertyHolder parent;
	private Map<String, Column[]> holderColumnOverride;
	private Map<String, Column[]> currentPropertyColumnOverride;
	private Map<String, JoinColumn[]> currentPropertyJoinColumnOverride;
	private Map<String, JoinColumn[]> holderJoinColumnOverride;
	private Map<String, JoinTable> currentPropertyJoinTableOverride;
	private Map<String, JoinTable> holderJoinTableOverride;
	private String path;
	private Mappings mappings;
	private Boolean isInIdClass;
	
	public AbstractPropertyHolder(
			String path,
			PropertyHolder parent,
			XClass clazzToProcess,
			Mappings mappings) {
		this.path = path;
		this.parent = (AbstractPropertyHolder) parent;
		this.mappings = mappings;
		buildHierarchyColumnOverride( clazzToProcess );
	}

	private void buildHierarchyColumnOverride(XClass element) {
		XClass current = element;
		Map<String, Column[]> columnOverride = new HashMap<String, Column[]>();
		while ( current != null && !mappings.getReflectionManager().toXClass( Object.class ).equals( current ) ) {
			if ( current.isAnnotationPresent( Entity.class ) || current.isAnnotationPresent( MappedSuperclass.class )
					|| current.isAnnotationPresent( Embeddable.class ) ) {
				//FIXME is embeddable override?
				Map<String, Column[]> currentOverride = buildColumnOverride( current, getPath() );
				//Map<String, JoinColumn[]> currentJoinOverride = buildJoinColumnOverride( current, getPath() );
				//Map<String, JoinTable> currentJoinTableOverride = buildJoinTableOverride( current, getPath() );
				currentOverride.putAll( columnOverride ); //subclasses have precedence over superclasses
				//currentJoinOverride.putAll( joinColumnOverride ); //subclasses have precedence over superclasses
				//currentJoinTableOverride.putAll( joinTableOverride ); //subclasses have precedence over superclasses
				columnOverride = currentOverride;
				//joinColumnOverride = currentJoinOverride;
				//joinTableOverride = currentJoinTableOverride;
			}
			current = current.getSuperclass();
		}

		holderColumnOverride = columnOverride.size() > 0 ? columnOverride : null;
		//holderJoinColumnOverride = joinColumnOverride.size() > 0 ? joinColumnOverride : null;
		//holderJoinTableOverride = joinTableOverride.size() > 0 ? joinTableOverride : null;
		
	}
	
	protected void setCurrentProperty(XProperty property) {
		if ( property == null ) {
			this.currentPropertyColumnOverride = null;
			this.currentPropertyJoinColumnOverride = null;
			this.currentPropertyJoinTableOverride = null;
		}
		else {
			this.currentPropertyColumnOverride = buildColumnOverride(
					property,
					getPath()
			);
			if ( this.currentPropertyColumnOverride.size() == 0 ) {
				this.currentPropertyColumnOverride = null;
			}
			this.currentPropertyJoinColumnOverride = buildJoinColumnOverride(
					property,
					getPath()
			);
			if ( this.currentPropertyJoinColumnOverride.size() == 0 ) {
				this.currentPropertyJoinColumnOverride = null;
			}
			this.currentPropertyJoinTableOverride = buildJoinTableOverride(
					property,
					getPath()
			);
			if ( this.currentPropertyJoinTableOverride.size() == 0 ) {
				this.currentPropertyJoinTableOverride = null;
			}
		}
	}
	
	private static Map<String, JoinTable> buildJoinTableOverride(XAnnotatedElement element, String path) {
		Map<String, JoinTable> tableOverride = new HashMap<String, JoinTable>();
		if ( element == null ) return tableOverride;
		AssociationOverride singleOverride = element.getAnnotation( AssociationOverride.class );
		AssociationOverrides multipleOverrides = element.getAnnotation( AssociationOverrides.class );
		AssociationOverride[] overrides;
		if ( singleOverride != null ) {
			overrides = new AssociationOverride[] { singleOverride };
		}
		else if ( multipleOverrides != null ) {
			overrides = multipleOverrides.value();
		}
		else {
			overrides = null;
		}

		if ( overrides != null ) {
			for (AssociationOverride depAttr : overrides) {
				if ( depAttr.joinColumns().length == 0 ) {
					tableOverride.put(
							StringHelper.qualify( path, depAttr.name() ),
							depAttr.joinTable()
					);
				}
			}
		}
		return tableOverride;
	}
	
	private static Map<String, JoinColumn[]> buildJoinColumnOverride(XAnnotatedElement element, String path) {
		Map<String, JoinColumn[]> columnOverride = new HashMap<String, JoinColumn[]>();
		if ( element == null ) return columnOverride;
		AssociationOverride singleOverride = element.getAnnotation( AssociationOverride.class );
		AssociationOverrides multipleOverrides = element.getAnnotation( AssociationOverrides.class );
		AssociationOverride[] overrides;
		if ( singleOverride != null ) {
			overrides = new AssociationOverride[] { singleOverride };
		}
		else if ( multipleOverrides != null ) {
			overrides = multipleOverrides.value();
		}
		else {
			overrides = null;
		}

		//fill overridden columns
		if ( overrides != null ) {
			for (AssociationOverride depAttr : overrides) {
				columnOverride.put(
						StringHelper.qualify( path, depAttr.name() ),
						depAttr.joinColumns()
				);
			}
		}
		return columnOverride;
	}

	private static Map<String, Column[]> buildColumnOverride(XAnnotatedElement element,
			String path) {
		Map<String,Column[]> columnOverride = new HashMap<String, Column[]>();
		if(element == null) return columnOverride;
		AttributeOverride singleOverride = element.getAnnotation( AttributeOverride.class );
		AttributeOverrides multipleOverrides = element.getAnnotation( AttributeOverrides.class );
		AttributeOverride[] overrides;
		if(singleOverride != null){
			overrides = new AttributeOverride[] { singleOverride };
		}
		else if ( multipleOverrides != null ) {
			overrides = multipleOverrides.value();
		}
		else {
			overrides = null;
		}
		//fill overridden columns
		if ( overrides != null ) {
			for (AttributeOverride depAttr : overrides) {
				columnOverride.put(
						StringHelper.qualify( path, depAttr.name() ),
						new Column[] { depAttr.column() }
				);
			}
		}
		return columnOverride;
	}
	
	public String getPath() {
		return path;
	}
	
	protected Mappings getMappings() {
		return mappings;
	}
	
	public boolean isInIdClass() {
		return isInIdClass != null ? isInIdClass : parent != null ? parent.isInIdClass() : false;
	}

	public void setInIdClass(Boolean isInIdClass) {
		this.isInIdClass = isInIdClass;
	}
	
	public Column[] getOverriddenColumn(String propertyName) {
		Column[] result = getExactOverriddenColumn( propertyName );
		if (result == null) {
			if ( result == null && propertyName.contains( ".collection&&element." ) ) {
				//support for non map collections where no prefix is needed
				//TODO cache the underlying regexp
				result = getExactOverriddenColumn( propertyName.replace( ".collection&&element.", "."  ) );
			}
		}
		return result;
	}
	
	private Column[] getExactOverriddenColumn(String propertyName) {
		Column[] override = null;
		if ( parent != null ) {
			override = parent.getExactOverriddenColumn( propertyName );
		}
		if ( override == null && currentPropertyColumnOverride != null ) {
			override = currentPropertyColumnOverride.get( propertyName );
		}
		if ( override == null && holderColumnOverride != null ) {
			override = holderColumnOverride.get( propertyName );
		}
		return override;
	}
	
	private JoinColumn[] getExactOverriddenJoinColumn(String propertyName) {
		JoinColumn[] override = null;
		if ( parent != null ) {
			override = parent.getExactOverriddenJoinColumn( propertyName );
		}
		if ( override == null && currentPropertyJoinColumnOverride != null ) {
			override = currentPropertyJoinColumnOverride.get( propertyName );
		}
		if ( override == null && holderJoinColumnOverride != null ) {
			override = holderJoinColumnOverride.get( propertyName );
		}
		return override;
	}
	
	public JoinColumn[] getOverriddenJoinColumn(String propertyName) {
		JoinColumn[] result =  getExactOverriddenJoinColumn( propertyName );
		if ( result == null && propertyName.contains( ".collection&&element." ) ) {
			result = getExactOverriddenJoinColumn( propertyName.replace( ".collection&&element.", "."  ) );
		}
		return result;
	}
	
	public JoinTable getJoinTable(XProperty property) {
		final String propertyName = StringHelper.qualify( getPath(), property.getName() );
		JoinTable result = getOverriddenJoinTable( propertyName );
		if (result == null) {
			result = property.getAnnotation( JoinTable.class );
		}
		return result;
	}
	
	public JoinTable getOverriddenJoinTable(String propertyName) {
		JoinTable result = getExactOverriddenJoinTable( propertyName );
		if ( result == null && propertyName.contains( ".collection&&element." ) ) {
			//support for non map collections where no prefix is needed
			//TODO cache the underlying regexp
			result = getExactOverriddenJoinTable( propertyName.replace( ".collection&&element.", "."  ) );
		}
		return result;
	}
	
	private JoinTable getExactOverriddenJoinTable(String propertyName) {
		JoinTable override = null;
		if ( parent != null ) {
			override = parent.getExactOverriddenJoinTable( propertyName );
		}
		if ( override == null && currentPropertyJoinTableOverride != null ) {
			override = currentPropertyJoinTableOverride.get( propertyName );
		}
		if ( override == null && holderJoinTableOverride != null ) {
			override = holderJoinTableOverride.get( propertyName );
		}
		return override;
	}
	
	public void setParentProperty(String parentProperty) {
		throw new AssertionFailure( "Setting the parent property to a non component" );
	}
}
