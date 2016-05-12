package com.zorm.config;

import java.util.Map;

import javax.persistence.OrderColumn;

import com.zorm.mapping.Join;

public class IndexColumn extends Ejb3Column{
	private int base;
	
	public IndexColumn(
			boolean isImplicit,
			String sqlType,
			int length,
			int precision,
			int scale,
			String name,
			boolean nullable,
			boolean unique,
			boolean insertable,
			boolean updatable,
			String secondaryTableName,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			Mappings mappings) {
		super();
		setImplicit( isImplicit );
		setSqlType( sqlType );
		setLength( length );
		setPrecision( precision );
		setScale( scale );
		setLogicalColumnName( name );
		setNullable( nullable );
		setUnique( unique );
		setInsertable( insertable );
		setUpdatable( updatable );
		setSecondaryTableName( secondaryTableName );
		setPropertyHolder( propertyHolder );
		setJoins( joins );
		setMappings( mappings );
		bind();
	}

	public int getBase() {
		return base;
	}

	public void setBase(int base) {
		this.base = base;
	}

	//JPA 2 @OrderColumn processing
	public static IndexColumn buildColumnFromAnnotation(
			OrderColumn ann,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			Mappings mappings) {
		IndexColumn column;
		if ( ann != null ) {
			String sqlType = BinderHelper.isEmptyAnnotationValue( ann.columnDefinition() ) ? null : ann.columnDefinition();
			String name = BinderHelper.isEmptyAnnotationValue( ann.name() ) ? inferredData.getPropertyName() + "_ORDER" : ann.name();
			//TODO move it to a getter based system and remove the constructor
// The JPA OrderColumn annotation defines no table element...
//			column = new IndexColumn(
//					false, sqlType, 0, 0, 0, name, ann.nullable(),
//					false, ann.insertable(), ann.updatable(), ann.table(),
//					secondaryTables, propertyHolder, mappings
//			);
			column = new IndexColumn(
					false, sqlType, 0, 0, 0, name, ann.nullable(),
					false, ann.insertable(), ann.updatable(), /*ann.table()*/null,
					secondaryTables, propertyHolder, mappings
			);
		}
		else {
			column = new IndexColumn(
					true, null, 0, 0, 0, null, true,
					false, true, true, null, null, propertyHolder, mappings
			);
		}
		return column;
	}

	//legacy @IndexColumn processing
	public static IndexColumn buildColumnFromAnnotation(
			com.zorm.annotations.IndexColumn ann,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Mappings mappings) {
		IndexColumn column;
		if ( ann != null ) {
			String sqlType = BinderHelper.isEmptyAnnotationValue( ann.columnDefinition() ) ? null : ann.columnDefinition();
			String name = BinderHelper.isEmptyAnnotationValue( ann.name() ) ? inferredData.getPropertyName() : ann.name();
			//TODO move it to a getter based system and remove the constructor
			column = new IndexColumn(
					false, sqlType, 0, 0, 0, name, ann.nullable(),
					false, true, true, null, null, propertyHolder, mappings
			);
			column.setBase( ann.base() );
		}
		else {
			column = new IndexColumn(
					true, null, 0, 0, 0, null, true,
					false, true, true, null, null, propertyHolder, mappings
			);
		}
		return column;
	}
}
