package com.zorm.config;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;

import com.zorm.annotations.ManyToMany;
import com.zorm.annotations.ManyToOne;
import com.zorm.annotations.OneToMany;
import com.zorm.annotations.OneToOne;
import com.zorm.annotations.reflection.XProperty;
import com.zorm.config.annotations.EntityBinder;
import com.zorm.config.annotations.Nullability;
import com.zorm.exception.AnnotationException;
import com.zorm.util.StringHelper;

public class ColumnsBuilder {
	private PropertyHolder propertyHolder;
	private Nullability nullability;
	private XProperty property;
	private PropertyData inferredData;
	private EntityBinder entityBinder;
	private Mappings mappings;
	private Ejb3Column[] columns;
	private Ejb3JoinColumn[] joinColumns;
	
	public ColumnsBuilder(
			PropertyHolder propertyHolder,
			Nullability nullability,
			XProperty property,
			PropertyData inferredData,
			EntityBinder entityBinder,
			Mappings mappings) {
		this.propertyHolder = propertyHolder;
		this.nullability = nullability;
		this.property = property;
		this.inferredData = inferredData;
		this.entityBinder = entityBinder;
		this.mappings = mappings;
	}
	
	Ejb3JoinColumn[] buildExplicitJoinColumns(XProperty property, PropertyData inferredData) {
		Ejb3JoinColumn[] joinColumns = null;
		{
			JoinColumn[] anns = null;

			if ( property.isAnnotationPresent( JoinColumn.class ) ) {
				anns = new JoinColumn[] { property.getAnnotation( JoinColumn.class ) };
			}
			else if ( property.isAnnotationPresent( JoinColumns.class ) ) {
				JoinColumns ann = property.getAnnotation( JoinColumns.class );
				anns = ann.value();
				int length = anns.length;
				if ( length == 0 ) {
					throw new AnnotationException( "Cannot bind an empty @JoinColumns" );
				}
			}
			if ( anns != null ) {
				joinColumns = Ejb3JoinColumn.buildJoinColumns(
						anns, null, entityBinder.getSecondaryTables(),
						propertyHolder, inferredData.getPropertyName(), mappings
				);
			}
		}
		return joinColumns;
	}

	public ColumnsBuilder extractMetadata() {
		columns = null;
		//实体关联的外键列
		joinColumns = buildExplicitJoinColumns(property, inferredData);
		if(property.isAnnotationPresent(Column.class)){
			Column ann = property.getAnnotation(Column.class);
			//从注解中解析出相关的列
			columns = Ejb3Column.buildColumnFromAnnotation(
					new Column[] { ann }, nullability, propertyHolder, inferredData, mappings
			);
		}

		if ( joinColumns == null &&
				( property.isAnnotationPresent( ManyToOne.class )
						|| property.isAnnotationPresent( OneToOne.class ) )
				) {
			joinColumns = buildDefaultJoinColumnsForXToOne(property, inferredData);
		}
		else if ( joinColumns == null &&
				( property.isAnnotationPresent( OneToMany.class ))
				)  {
			OneToMany oneToMany = property.getAnnotation( OneToMany.class );
			String mappedBy = oneToMany != null ?
					oneToMany.mappedBy() :
					"";
			joinColumns = Ejb3JoinColumn.buildJoinColumns(
					null,
					mappedBy, entityBinder.getSecondaryTables(),
					propertyHolder, inferredData.getPropertyName(), mappings
			);
		}
		
		if ( columns == null && !property.isAnnotationPresent( ManyToMany.class ) ) {
			//useful for collection of embedded elements
			columns = Ejb3Column.buildColumnFromAnnotation(
					null, nullability, propertyHolder, inferredData, mappings
			);
		}
		
		if(nullability == Nullability.FORCED_NOT_NULL){
			//强制不能为空
			for(Ejb3Column col : columns){
				col.forceNotNull();
			}
		}
		return this;
	}
	
	Ejb3JoinColumn[] buildDefaultJoinColumnsForXToOne(XProperty property, PropertyData inferredData) {
		Ejb3JoinColumn[] joinColumns;
		JoinTable joinTableAnn = propertyHolder.getJoinTable( property );
		if ( joinTableAnn != null ) {
			joinColumns = Ejb3JoinColumn.buildJoinColumns(
					joinTableAnn.inverseJoinColumns(), null, entityBinder.getSecondaryTables(),
					propertyHolder, inferredData.getPropertyName(), mappings
			);
			if ( StringHelper.isEmpty( joinTableAnn.name() ) ) {
				throw new AnnotationException(
						"JoinTable.name() on a @ToOne association has to be explicit: "
								+ BinderHelper.getPath( propertyHolder, inferredData )
				);
			}
		}
		else {
			OneToOne oneToOneAnn = property.getAnnotation( OneToOne.class );
			String mappedBy = oneToOneAnn != null ?
					oneToOneAnn.mappedBy() :
					null;
			joinColumns = Ejb3JoinColumn.buildJoinColumns(
					null,
					mappedBy, entityBinder.getSecondaryTables(),
					propertyHolder, inferredData.getPropertyName(), mappings
			);
		}
		return joinColumns;
	}

	public Ejb3Column[] getColumns() {
		return columns;
	}

	public Ejb3JoinColumn[] getJoinColumns() {
		return joinColumns;
	}
}
