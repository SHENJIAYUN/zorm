package com.zorm.config.annotations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.UniqueConstraint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.config.BinderHelper;
import com.zorm.config.Ejb3JoinColumn;
import com.zorm.config.Mappings;
import com.zorm.config.NamingStrategy;
import com.zorm.config.ObjectNameNormalizer;
import com.zorm.config.ObjectNameSource;
import com.zorm.config.UniqueConstraintHolder;
import com.zorm.exception.AnnotationException;
import com.zorm.mapping.Collection;
import com.zorm.mapping.Column;
import com.zorm.mapping.JoinedSubclass;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Property;
import com.zorm.mapping.SimpleValue;
import com.zorm.mapping.Table;
import com.zorm.mapping.Value;
import com.zorm.util.CollectionHelper;
import com.zorm.util.StringHelper;

public class TableBinder {

	private static final Log log = LogFactory.getLog(TableBinder.class);
	
	Mappings mappings;
	private String name;
	private String schema;
	private String catalog;
	private String ownerEntityTable;
	private String associatedEntityTable;
	private String propertyName;
	private String ownerEntity;
	private String associatedEntity;
	private boolean isJPA2ElementCollection;
	private boolean isAbstract;
	String constraints;
	Table denormalizedSuperTable;
	private List<UniqueConstraintHolder> uniqueConstraints;
	
	public void setSchema(String schema) {
		this.schema = schema;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}
	
	public void setConstraints(String constraints) {
		this.constraints = constraints;
	}

	public void setDenormalizedSuperTable(Table denormalizedSuperTable) {
		this.denormalizedSuperTable = denormalizedSuperTable;
	}
	
	public void setAbstract(boolean anAbstract) {
		isAbstract = anAbstract;
	}
	
	public static List<UniqueConstraintHolder> buildUniqueConstraintHolders(
			UniqueConstraint[] annotations) {
		List<UniqueConstraintHolder> result;
		if ( annotations == null || annotations.length == 0 ) {
			result = java.util.Collections.emptyList();
		}
		else {
			result = new ArrayList<UniqueConstraintHolder>( CollectionHelper.determineProperSizing( annotations.length ) );
			for ( UniqueConstraint uc : annotations ) {
				result.add(
						new UniqueConstraintHolder()
								.setName( uc.name() )
								.setColumns( uc.columnNames() )
				);
			}
		}
		return result;
	}

	/*
	 * 
	 */
	public static Table buildAndFillTable(String schema, String catalog,
			ObjectNameSource nameSource,
			ObjectNameNormalizer.NamingStrategyHelper namingStrategyHelper,
			Boolean isAbstract, 
			List<UniqueConstraintHolder> uniqueConstraints,
			Table denormalizedSuperTable, 
			Mappings mappings,
			String subselect) {
		schema = BinderHelper.isEmptyAnnotationValue( schema ) ? mappings.getSchemaName() : schema;
		catalog = BinderHelper.isEmptyAnnotationValue( catalog ) ? mappings.getCatalogName() : catalog;
		
		//获取真实数据库表名
		String realTableName = mappings.getObjectNameNormalizer().normalizeDatabaseIdentifier(
				nameSource.getExplicitName(),
				namingStrategyHelper
		);
		final Table table;
		if(denormalizedSuperTable!=null){
			table=mappings.addDenormalizedTable(
					  schema,
					  catalog,
					  realTableName,
					  isAbstract,
					  subselect,
					  denormalizedSuperTable
					);
		}else{
			//将类名对应的表存储在mappings中
			table = mappings.addTable(
					 schema,
					 catalog,
					 realTableName,
					 subselect,
					 isAbstract
					);
		}
		
		if ( uniqueConstraints != null && uniqueConstraints.size() > 0 ) {
			mappings.addUniqueConstraintHolders( table, uniqueConstraints );
		}
		// logicalName is null if we are in the second pass
		final String logicalName = nameSource.getLogicalName();
		if ( logicalName != null ) {
			mappings.addTableBinding( schema, catalog, logicalName, realTableName, denormalizedSuperTable );
		}
		return table;
	}
	
	@SuppressWarnings("rawtypes")
	public static void bindFk(
			PersistentClass referencedEntity,
			PersistentClass destinationEntity,
			Ejb3JoinColumn[] columns,
			SimpleValue value,
			boolean unique,
			Mappings mappings) {
		PersistentClass associatedClass;
		if ( destinationEntity != null ) {
			associatedClass = destinationEntity;
		}
		else {
			associatedClass = columns[0].getPropertyHolder() == null
					? null
					: columns[0].getPropertyHolder().getPersistentClass();
		}
		final String mappedByProperty = columns[0].getMappedBy();
		if ( StringHelper.isNotEmpty( mappedByProperty ) ) {
			/**
			 * Get the columns of the mapped-by property
			 * copy them and link the copy to the actual value
			 */
			log.debug( "Retrieving property "+associatedClass.getEntityName()+"."+ mappedByProperty );

			final Property property = associatedClass.getRecursiveProperty( columns[0].getMappedBy() );
			Iterator mappedByColumns;
			if ( property.getValue() instanceof Collection ) {
				Collection collection = ( (Collection) property.getValue() );
				Value element = collection.getElement();
				if ( element == null ) {
					throw new AnnotationException(
							"Illegal use of mappedBy on both sides of the relationship: "
									+ associatedClass.getEntityName() + "." + mappedByProperty
					);
				}
				mappedByColumns = element.getColumnIterator();
			}
			else {
				mappedByColumns = property.getValue().getColumnIterator();
			}
			while ( mappedByColumns.hasNext() ) {
				Column column = (Column) mappedByColumns.next();
				columns[0].overrideFromReferencedColumnIfNecessary( column );
				columns[0].linkValueUsingAColumnCopy( column, value );
			}
		}
		else if ( columns[0].isImplicit() ) {
			/**
			 * if columns are implicit, then create the columns based on the
			 * referenced entity id columns
			 */
			Iterator idColumns;
			if ( referencedEntity instanceof JoinedSubclass ) {
				idColumns = referencedEntity.getKey().getColumnIterator();
			}
			else {
				idColumns = referencedEntity.getIdentifier().getColumnIterator();
			}
			while ( idColumns.hasNext() ) {
//				Column column = (Column) idColumns.next();
//				columns[0].overrideFromReferencedColumnIfNecessary( column );
//				columns[0].linkValueUsingDefaultColumnNaming( column, referencedEntity, value );
			}
		}
		else {
			int fkEnum = Ejb3JoinColumn.checkReferencedColumnsType( columns, referencedEntity, mappings );

			if ( Ejb3JoinColumn.NON_PK_REFERENCE == fkEnum ) {

			}
			else {
				if ( Ejb3JoinColumn.NO_REFERENCE == fkEnum ) {
					//implicit case, we hope PK and FK columns are in the same order
					if ( columns.length != referencedEntity.getIdentifier().getColumnSpan() ) {
						throw new AnnotationException(
								"A Foreign key refering " + referencedEntity.getEntityName()
										+ " from " + associatedClass.getEntityName()
										+ " has the wrong number of column. should be " + referencedEntity.getIdentifier()
										.getColumnSpan()
						);
					}
					linkJoinColumnWithValueOverridingNameIfImplicit(
							referencedEntity,
							referencedEntity.getIdentifier().getColumnIterator(),
							columns,
							value
					);
				}
				else {
					//explicit referencedColumnName
					Iterator idColItr = referencedEntity.getKey().getColumnIterator();
					com.zorm.mapping.Column col;
					Table table = referencedEntity.getTable(); //works cause the pk has to be on the primary table
					if ( !idColItr.hasNext() ) {
						log.debug( "No column in the identifier!" );
					}
					while ( idColItr.hasNext() ) {
						boolean match = false;
						//for each PK column, find the associated FK column.
						col = (com.zorm.mapping.Column) idColItr.next();
						for (Ejb3JoinColumn joinCol : columns) {
							String referencedColumn = joinCol.getReferencedColumn();
							referencedColumn = mappings.getPhysicalColumnName( referencedColumn, table );
							//In JPA 2 referencedColumnName is case insensitive
							if ( referencedColumn.equalsIgnoreCase( col.getQuotedName() ) ) {
								//proper join column
								if ( joinCol.isNameDeferred() ) {
									joinCol.linkValueUsingDefaultColumnNaming(
											col, referencedEntity, value
									);
								}
								else {
									joinCol.linkWithValue( value );
								}
								joinCol.overrideFromReferencedColumnIfNecessary( col );
								match = true;
								break;
							}
						}
						if ( !match ) {
							throw new AnnotationException(
									"Column name " + col.getName() + " of "
											+ referencedEntity.getEntityName() + " not found in JoinColumns.referencedColumnName"
							);
						}
					}
				}
			}
		}
		value.createForeignKey();
		if ( unique ) {
//			createUniqueConstraint( value );
		}
	}
	
	public static void linkJoinColumnWithValueOverridingNameIfImplicit(
			PersistentClass referencedEntity,
			Iterator columnIterator,
			Ejb3JoinColumn[] columns,
			SimpleValue value) {
		for (Ejb3JoinColumn joinCol : columns) {
			Column synthCol = (Column) columnIterator.next();
			if ( joinCol.isNameDeferred() ) {
				//this has to be the default value
//				joinCol.linkValueUsingDefaultColumnNaming( synthCol, referencedEntity, value );
			}
			else {
				joinCol.linkWithValue( value );
				joinCol.overrideFromReferencedColumnIfNecessary( synthCol );
			}
		}
	}

	public void setMappings(Mappings mappings) {
		this.mappings = mappings;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setDefaultName(
			String ownerEntity, String ownerEntityTable, String associatedEntity, String associatedEntityTable,
			String propertyName
	) {
		this.ownerEntity = ownerEntity;
		this.ownerEntityTable = ownerEntityTable;
		this.associatedEntity = associatedEntity;
		this.associatedEntityTable = associatedEntityTable;
		this.propertyName = propertyName;
		this.name = null;
	}

	public void setJPA2ElementCollection(boolean isJPA2ElementCollection) {
		this.isJPA2ElementCollection = isJPA2ElementCollection;
	}
	
	private static class AssociationTableNameSource implements ObjectNameSource {
		private final String explicitName;
		private final String logicalName;

		private AssociationTableNameSource(String explicitName, String logicalName) {
			this.explicitName = explicitName;
			this.logicalName = logicalName;
		}

		public String getExplicitName() {
			return explicitName;
		}

		public String getLogicalName() {
			return logicalName;
		}
	}

	private ObjectNameSource buildNameContext(String unquotedOwnerTable, String unquotedAssocTable) {
		String logicalName = mappings.getNamingStrategy().logicalCollectionTableName(
				name,
				unquotedOwnerTable,
				unquotedAssocTable,
				propertyName
		);
		if ( StringHelper.isQuoted( ownerEntityTable ) || StringHelper.isQuoted( associatedEntityTable ) ) {
			logicalName = StringHelper.quote( logicalName );
		}

		return new AssociationTableNameSource( name, logicalName );
	}
	
	public void setUniqueConstraints(UniqueConstraint[] uniqueConstraints) {
		this.uniqueConstraints = TableBinder.buildUniqueConstraintHolders( uniqueConstraints );
	}
	
	public Table bind() {
		final String unquotedOwnerTable = StringHelper.unquote( ownerEntityTable );
		final String unquotedAssocTable = StringHelper.unquote( associatedEntityTable );

		final String ownerObjectName = isJPA2ElementCollection && ownerEntity != null ?
				StringHelper.unqualify( ownerEntity ) : unquotedOwnerTable;
		final ObjectNameSource nameSource = buildNameContext(
				ownerObjectName,
				unquotedAssocTable );

		final boolean ownerEntityTableQuoted = StringHelper.isQuoted( ownerEntityTable );
		final boolean associatedEntityTableQuoted = StringHelper.isQuoted( associatedEntityTable );
		final ObjectNameNormalizer.NamingStrategyHelper namingStrategyHelper = new ObjectNameNormalizer.NamingStrategyHelper() {
			public String determineImplicitName(NamingStrategy strategy) {

				final String strategyResult = strategy.collectionTableName(
						ownerEntity,
						ownerObjectName,
						associatedEntity,
						unquotedAssocTable,
						propertyName

				);
				return ownerEntityTableQuoted || associatedEntityTableQuoted
						? StringHelper.quote( strategyResult )
						: strategyResult;
			}

			public String handleExplicitName(NamingStrategy strategy, String name) {
				return strategy.tableName( name );
			}
		};

		return buildAndFillTable(
				schema,
				catalog,
				nameSource,
				namingStrategyHelper,
				isAbstract,
				uniqueConstraints,
				constraints,
				denormalizedSuperTable,
				mappings,
				null
		);
	}
	
	public static Table buildAndFillTable(
			String schema,
			String catalog,
			ObjectNameSource nameSource,
			ObjectNameNormalizer.NamingStrategyHelper namingStrategyHelper,
			boolean isAbstract,
			List<UniqueConstraintHolder> uniqueConstraints,
			String constraints,
			Table denormalizedSuperTable,
			Mappings mappings,
			String subselect) {
		schema = BinderHelper.isEmptyAnnotationValue( schema ) ? mappings.getSchemaName() : schema;
		catalog = BinderHelper.isEmptyAnnotationValue( catalog ) ? mappings.getCatalogName() : catalog;

		String realTableName = mappings.getObjectNameNormalizer().normalizeDatabaseIdentifier(
				nameSource.getExplicitName(),
				namingStrategyHelper
		);

		final Table table;
		if ( denormalizedSuperTable != null ) {
			table = mappings.addDenormalizedTable(
					schema,
					catalog,
					realTableName,
					isAbstract,
					subselect,
					denormalizedSuperTable
			);
		}
		else {
			table = mappings.addTable(
					schema,
					catalog,
					realTableName,
					subselect,
					isAbstract
			);
		}

		if ( uniqueConstraints != null && uniqueConstraints.size() > 0 ) {
			mappings.addUniqueConstraintHolders( table, uniqueConstraints );
		}

		if ( constraints != null ) table.addCheckConstraint( constraints );

		// logicalName is null if we are in the second pass
		final String logicalName = nameSource.getLogicalName();
		if ( logicalName != null ) {
			mappings.addTableBinding( schema, catalog, logicalName, realTableName, denormalizedSuperTable );
		}
		return table;
	}

}
