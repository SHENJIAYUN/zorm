package com.zorm.config;

import java.util.*;

import javax.persistence.JoinColumn;
import javax.persistence.PrimaryKeyJoinColumn;

import com.zorm.annotations.reflection.XClass;
import com.zorm.exception.AnnotationException;
import com.zorm.exception.MappingException;
import com.zorm.exception.RecoverableException;
import com.zorm.mapping.Column;
import com.zorm.mapping.Join;
import com.zorm.mapping.KeyValue;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.SimpleValue;
import com.zorm.mapping.Table;
import com.zorm.mapping.Value;
import com.zorm.util.StringHelper;

public class Ejb3JoinColumn extends Ejb3Column{
	private String referencedColumn;
	private String mappedBy;
	//property name on the mapped by side if any
	private String mappedByPropertyName;
	//table name on the mapped by side if any
	private String mappedByTableName;
	private String mappedByEntityName;
	private boolean JPA2ElementCollection;
	private String manyToManyOwnerSideEntityName;
	
	
	public static final int NO_REFERENCE = 0;
	public static final int PK_REFERENCE = 1;
	public static final int NON_PK_REFERENCE = 2;
	
	private Ejb3JoinColumn(
			String sqlType,
			String name,
			boolean nullable,
			boolean unique,
			boolean insertable,
			boolean updatable,
			String referencedColumn,
			String secondaryTable,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			String mappedBy,
			boolean isImplicit,
			Mappings mappings) {
		super();
		setImplicit( isImplicit );
		setSqlType( sqlType );
		setLogicalColumnName( name );
		setNullable( nullable );
		setUnique( unique );
		setInsertable( insertable );
		setUpdatable( updatable );
		setSecondaryTableName( secondaryTable );
		setPropertyHolder( propertyHolder );
		setJoins( joins );
		setMappings( mappings );
		setPropertyName( BinderHelper.getRelativePath( propertyHolder, propertyName ) );
		bind();
		this.referencedColumn = referencedColumn;
		this.mappedBy = mappedBy;
	}
	
	private Ejb3JoinColumn() {
		setMappedBy( BinderHelper.ANNOTATION_STRING_DEFAULT );
	}
	
	public String getManyToManyOwnerSideEntityName() {
		return manyToManyOwnerSideEntityName;
	}
	public void setManyToManyOwnerSideEntityName(
			String manyToManyOwnerSideEntityName) {
		this.manyToManyOwnerSideEntityName = manyToManyOwnerSideEntityName;
	}
	public String getReferencedColumn() {
		return referencedColumn;
	}
	public void setReferencedColumn(String referencedColumn) {
		this.referencedColumn = referencedColumn;
	}
	public String getMappedBy() {
		return mappedBy;
	}
	public void setMappedBy(String mappedBy) {
		this.mappedBy = mappedBy;
	}
	public String getMappedByPropertyName() {
		return mappedByPropertyName;
	}
	public void setMappedByPropertyName(String mappedByPropertyName) {
		this.mappedByPropertyName = mappedByPropertyName;
	}
	public String getMappedByTableName() {
		return mappedByTableName;
	}
	public void setMappedByTableName(String mappedByTableName) {
		this.mappedByTableName = mappedByTableName;
	}
	public String getMappedByEntityName() {
		return mappedByEntityName;
	}
	public void setMappedByEntityName(String mappedByEntityName) {
		this.mappedByEntityName = mappedByEntityName;
	}
	public boolean isJPA2ElementCollection() {
		return JPA2ElementCollection;
	}
	public void setJPA2ElementCollection(boolean jPA2ElementCollection) {
		JPA2ElementCollection = jPA2ElementCollection;
	}
	
	public static Ejb3JoinColumn buildJoinColumn(
			PrimaryKeyJoinColumn pkJoinAnn,
			JoinColumn joinAnn,
			Value identifier,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			Mappings mappings) {
		Column col = (Column) identifier.getColumnIterator().next();
		String defaultName = mappings.getLogicalColumnName( col.getQuotedName(), identifier.getTable() );
		if ( pkJoinAnn != null || joinAnn != null ) {
			String colName;
			String columnDefinition;
			String referencedColumnName;
			if ( pkJoinAnn != null ) {
				colName = pkJoinAnn.name();
				columnDefinition = pkJoinAnn.columnDefinition();
				referencedColumnName = pkJoinAnn.referencedColumnName();
			}
			else {
				colName = joinAnn.name();
				columnDefinition = joinAnn.columnDefinition();
				referencedColumnName = joinAnn.referencedColumnName();
			}

			String sqlType = "".equals( columnDefinition )
					? null
					: mappings.getObjectNameNormalizer().normalizeIdentifierQuoting( columnDefinition );
			String name = "".equals( colName )
					? defaultName
					: colName;
			name = mappings.getObjectNameNormalizer().normalizeIdentifierQuoting( name );
			return new Ejb3JoinColumn(
					sqlType,
					name, false, false,
					true, true,
					referencedColumnName,
					null, joins,
					propertyHolder, null, null, false, mappings
			);
		}
		else {
			defaultName = mappings.getObjectNameNormalizer().normalizeIdentifierQuoting( defaultName );
			return new Ejb3JoinColumn(
					(String) null, defaultName,
					false, false, true, true, null, (String) null,
					joins, propertyHolder, null, null, true, mappings
			);
		}
	}

	public static Ejb3JoinColumn[] buildJoinColumns(
			JoinColumn[] anns,
			String mappedBy, 
			Map<String, Join> joins,
			PropertyHolder propertyHolder, 
			String propertyName,
			Mappings mappings) {
		return buildJoinColumnsWithDefaultColumnSuffix(
				anns, mappedBy, joins, propertyHolder, propertyName, "", mappings
		);
	}
	
	public static Ejb3JoinColumn[] buildJoinColumnsWithDefaultColumnSuffix(
			JoinColumn[] anns,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			String suffixForDefaultColumnName,
			Mappings mappings) {
		JoinColumn[] actualColumns = propertyHolder.getOverriddenJoinColumn(
				StringHelper.qualify( propertyHolder.getPath(), propertyName )
		);
		if ( actualColumns == null ) {
			actualColumns = anns;
		}
		if ( actualColumns == null || actualColumns.length == 0 ) {
			return new Ejb3JoinColumn[] {
					buildJoinColumn(
							(JoinColumn) null,
							mappedBy,
							joins,
							propertyHolder,
							propertyName,
							suffixForDefaultColumnName,
							mappings )
			};
		}
		else {
			int size = actualColumns.length;
			Ejb3JoinColumn[] result = new Ejb3JoinColumn[size];
			for (int index = 0; index < size; index++) {
				result[index] = buildJoinColumn(
						actualColumns[index],
						mappedBy,
						joins,
						propertyHolder,
						propertyName,
						suffixForDefaultColumnName,
						mappings
				);
			}
			return result;
		}
	}
	
	public void setJoinAnnotation(JoinColumn annJoin, String defaultName) {
		if ( annJoin == null ) {
			setImplicit( true );
		}
		else {
			setImplicit( false );
			if ( !BinderHelper.isEmptyAnnotationValue( annJoin.columnDefinition() ) ) setSqlType( annJoin.columnDefinition() );
			if ( !BinderHelper.isEmptyAnnotationValue( annJoin.name() ) ) setLogicalColumnName( annJoin.name() );
			setNullable( annJoin.nullable() );
			setUnique( annJoin.unique() );
			setInsertable( annJoin.insertable() );
			setUpdatable( annJoin.updatable() );
			setReferencedColumn( annJoin.referencedColumnName() );
			setSecondaryTableName( annJoin.table() );
		}
	}
	
	private static Ejb3JoinColumn buildJoinColumn(
			JoinColumn ann,
			String mappedBy, Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			String suffixForDefaultColumnName,
			Mappings mappings) {
		if ( ann != null ) {
			if ( BinderHelper.isEmptyAnnotationValue( mappedBy ) ) {
				throw new AnnotationException(
						"Illegal attempt to define a @JoinColumn with a mappedBy association: "
								+ BinderHelper.getRelativePath( propertyHolder, propertyName )
				);
			}
			Ejb3JoinColumn joinColumn = new Ejb3JoinColumn();
			joinColumn.setJoinAnnotation( ann, null );
			if ( StringHelper.isEmpty( joinColumn.getLogicalColumnName() )
				&& ! StringHelper.isEmpty( suffixForDefaultColumnName ) ) {
				joinColumn.setLogicalColumnName( propertyName + suffixForDefaultColumnName );
			}
			joinColumn.setJoins( joins );
			joinColumn.setPropertyHolder( propertyHolder );
			joinColumn.setPropertyName( BinderHelper.getRelativePath( propertyHolder, propertyName ) );
			joinColumn.setImplicit( false );
			joinColumn.setMappings( mappings );
			joinColumn.bind();
			return joinColumn;
		}
		else {
			Ejb3JoinColumn joinColumn = new Ejb3JoinColumn();
			joinColumn.setMappedBy( mappedBy );
			joinColumn.setJoins( joins );
			joinColumn.setPropertyHolder( propertyHolder );
			joinColumn.setPropertyName(
					BinderHelper.getRelativePath( propertyHolder, propertyName )
			);
			// property name + suffix is an "explicit" column name
			if ( !StringHelper.isEmpty( suffixForDefaultColumnName ) ) {
				joinColumn.setLogicalColumnName( propertyName + suffixForDefaultColumnName );
				joinColumn.setImplicit( false );
			}
			else {
				joinColumn.setImplicit( true );
			}
			joinColumn.setMappings( mappings );
			joinColumn.bind();
			return joinColumn;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static int checkReferencedColumnsType(
			Ejb3JoinColumn[] columns,
			PersistentClass referencedEntity,
			Mappings mappings) {
		//convenient container to find whether a column is an id one or not
		Set<Column> idColumns = new HashSet<Column>();
		Iterator idColumnsIt = referencedEntity.getKey().getColumnIterator();
		while ( idColumnsIt.hasNext() ) {
			idColumns.add( (Column) idColumnsIt.next() );
		}

		boolean isFkReferencedColumnName = false;
		boolean noReferencedColumn = true;
		//build the list of potential tables
		if ( columns.length == 0 ) return NO_REFERENCE; //shortcut
		Object columnOwner = BinderHelper.findColumnOwner(
				referencedEntity, columns[0].getReferencedColumn(), mappings
		);
		if ( columnOwner == null ) {
			try {
				throw new MappingException(
						"Unable to find column with logical name: "
								+ columns[0].getReferencedColumn() + " in " + referencedEntity.getTable() + " and its related "
								+ "supertables and secondary tables"
				);
			}
			catch (MappingException e) {
				throw new RecoverableException(e);
			}
		}
		Table matchingTable = columnOwner instanceof PersistentClass ?
				( (PersistentClass) columnOwner ).getTable() :
				( (Join) columnOwner ).getTable();
		//check each referenced column
		for (Ejb3JoinColumn ejb3Column : columns) {
			String logicalReferencedColumnName = ejb3Column.getReferencedColumn();
			if ( StringHelper.isNotEmpty( logicalReferencedColumnName ) ) {
				String referencedColumnName;
				try {
					referencedColumnName = mappings.getPhysicalColumnName( logicalReferencedColumnName, matchingTable );
				}
				catch (MappingException me) {
					//rewrite the exception
					throw new MappingException(
							"Unable to find column with logical name: "
									+ logicalReferencedColumnName + " in " + matchingTable.getName()
					);
				}
				noReferencedColumn = false;
				Column refCol = new Column( referencedColumnName );
				boolean contains = idColumns.contains( refCol );
				if ( !contains ) {
					isFkReferencedColumnName = true;
					break; //we know the state
				}
			}
		}
		if ( isFkReferencedColumnName ) {
			return NON_PK_REFERENCE;
		}
		else if ( noReferencedColumn ) {
			return NO_REFERENCE;
		}
		else if ( idColumns.size() != columns.length ) {
			//reference use PK but is a subset or a superset
			return NON_PK_REFERENCE;
		}
		else {
			return PK_REFERENCE;
		}
	}
	
	@Override
    protected void addColumnBinding(SimpleValue value) {
		if ( StringHelper.isEmpty( mappedBy ) ) {
			String unquotedLogColName = StringHelper.unquote( getLogicalColumnName() );
			String unquotedRefColumn = StringHelper.unquote( getReferencedColumn() );
			String logicalColumnName = getMappings().getNamingStrategy()
					.logicalCollectionColumnName( unquotedLogColName, getPropertyName(), unquotedRefColumn );
			if ( StringHelper.isQuoted( getLogicalColumnName() ) || StringHelper.isQuoted( getLogicalColumnName() ) ) {
				logicalColumnName = StringHelper.quote( logicalColumnName );
			}
			getMappings().addColumnBinding( logicalColumnName, getMappingColumn(), value.getTable() );
		}
	}

	public void overrideFromReferencedColumnIfNecessary(com.zorm.mapping.Column column) {
		if (getMappingColumn() != null) {
			// columnDefinition can also be specified using @JoinColumn, hence we have to check
			// whether it is set or not
			if ( StringHelper.isEmpty( sqlType ) ) {
				sqlType = column.getSqlType();
				getMappingColumn().setSqlType( sqlType );
			}

			// these properties can only be applied on the referenced column - we can just take them over
			getMappingColumn().setLength(column.getLength());
			getMappingColumn().setPrecision(column.getPrecision());
			getMappingColumn().setScale(column.getScale());
		}
	}

		public void linkValueUsingDefaultColumnNaming(
			Column referencedColumn,
			PersistentClass referencedEntity,
			SimpleValue value) {
		String columnName;
		String logicalReferencedColumn = getMappings().getLogicalColumnName(
				referencedColumn.getQuotedName(), referencedEntity.getTable()
		);
		columnName = buildDefaultColumnName( referencedEntity, logicalReferencedColumn );
		//yuk side effect on an implicit column
		setLogicalColumnName( columnName );
		setReferencedColumn( logicalReferencedColumn );
		initMappingColumn(
				columnName,
				null, referencedColumn.getLength(),
				referencedColumn.getPrecision(),
				referencedColumn.getScale(),
				getMappingColumn() != null ? getMappingColumn().isNullable() : false,
				referencedColumn.getSqlType(),
				getMappingColumn() != null ? getMappingColumn().isUnique() : false,
			    false
		);
		linkWithValue( value );
	}
		
		private String buildDefaultColumnName(PersistentClass referencedEntity, String logicalReferencedColumn) {
			String columnName;
			boolean mappedBySide = mappedByTableName != null || mappedByPropertyName != null;
			boolean ownerSide = getPropertyName() != null;

			Boolean isRefColumnQuoted = StringHelper.isQuoted( logicalReferencedColumn );
			String unquotedLogicalReferenceColumn = isRefColumnQuoted ?
					StringHelper.unquote( logicalReferencedColumn ) :
					logicalReferencedColumn;

			if ( mappedBySide ) {
				String unquotedMappedbyTable = StringHelper.unquote( mappedByTableName );
				final String ownerObjectName = JPA2ElementCollection && mappedByEntityName != null ?
					StringHelper.unqualify( mappedByEntityName ) : unquotedMappedbyTable;
				columnName = getMappings().getNamingStrategy().foreignKeyColumnName(
						mappedByPropertyName,
						mappedByEntityName,
						ownerObjectName,
						unquotedLogicalReferenceColumn
				);
				//one element was quoted so we quote
				if ( isRefColumnQuoted || StringHelper.isQuoted( mappedByTableName ) ) {
					columnName = StringHelper.quote( columnName );
				}
			}
			else if ( ownerSide ) {
				String logicalTableName = getMappings().getLogicalTableName( referencedEntity.getTable() );
				String unquotedLogicalTableName = StringHelper.unquote( logicalTableName );
				columnName = getMappings().getNamingStrategy().foreignKeyColumnName(
						getPropertyName(),
						referencedEntity.getEntityName(),
						unquotedLogicalTableName,
						unquotedLogicalReferenceColumn
				);
				//one element was quoted so we quote
				if ( isRefColumnQuoted || StringHelper.isQuoted( logicalTableName ) ) {
					columnName = StringHelper.quote( columnName );
				}
			}
			else {
				//is an intra-entity hierarchy table join so copy the name by default
				String logicalTableName = getMappings().getLogicalTableName( referencedEntity.getTable() );
				String unquotedLogicalTableName = StringHelper.unquote( logicalTableName );
				columnName = getMappings().getNamingStrategy().joinKeyColumnName(
						unquotedLogicalReferenceColumn,
						unquotedLogicalTableName
				);
				//one element was quoted so we quote
				if ( isRefColumnQuoted || StringHelper.isQuoted( logicalTableName ) ) {
					columnName = StringHelper.quote( columnName );
				}
			}
			return columnName;
		}

		public static Ejb3JoinColumn[] buildJoinTableJoinColumns(
				JoinColumn[] annJoins, 
				Map<String, Join> secondaryTables,
				PropertyHolder propertyHolder,
				String propertyName, 
				String mappedBy, 
				Mappings mappings) {
			Ejb3JoinColumn[] joinColumns;
			if ( annJoins == null ) {
				Ejb3JoinColumn currentJoinColumn = new Ejb3JoinColumn();
				currentJoinColumn.setImplicit( true );
				currentJoinColumn.setNullable( false ); //I break the spec, but it's for good
				currentJoinColumn.setPropertyHolder( propertyHolder );
				currentJoinColumn.setJoins( secondaryTables );
				currentJoinColumn.setMappings( mappings );
				currentJoinColumn.setPropertyName(
						BinderHelper.getRelativePath( propertyHolder, propertyName )
				);
				currentJoinColumn.setMappedBy( mappedBy );
				currentJoinColumn.bind();

				joinColumns = new Ejb3JoinColumn[] {
						currentJoinColumn

				};
			}
			else {
				joinColumns = new Ejb3JoinColumn[annJoins.length];
				JoinColumn annJoin;
				int length = annJoins.length;
				for (int index = 0; index < length; index++) {
					annJoin = annJoins[index];
					Ejb3JoinColumn currentJoinColumn = new Ejb3JoinColumn();
					currentJoinColumn.setImplicit( true );
					currentJoinColumn.setPropertyHolder( propertyHolder );
					currentJoinColumn.setJoins( secondaryTables );
					currentJoinColumn.setMappings( mappings );
					currentJoinColumn.setPropertyName( BinderHelper.getRelativePath( propertyHolder, propertyName ) );
					currentJoinColumn.setMappedBy( mappedBy );
					currentJoinColumn.setJoinAnnotation( annJoin, propertyName );
					currentJoinColumn.setNullable( false ); //I break the spec, but it's for good
					//done after the annotation to override it
					currentJoinColumn.bind();
					joinColumns[index] = currentJoinColumn;
				}
			}
			return joinColumns;
		}

		public void setPersistentClass(
				PersistentClass persistentClass,
				Map<String, Join> joins,
				Map<XClass, InheritanceState> inheritanceStatePerClass) {
			this.propertyHolder = PropertyHolderBuilder.buildPropertyHolder( persistentClass, joins, getMappings(), inheritanceStatePerClass );
		}

		public void setMappedBy(String entityName, String logicalTableName, String mappedByProperty) {
			this.mappedByEntityName = entityName;
			this.mappedByTableName = logicalTableName;
			this.mappedByPropertyName = mappedByProperty;
		}

		public void linkValueUsingAColumnCopy(Column column, SimpleValue value) {
			initMappingColumn(
					//column.getName(),
					column.getQuotedName(),
					null, column.getLength(),
					column.getPrecision(),
					column.getScale(),
					getMappingColumn().isNullable(),
					column.getSqlType(),
					getMappingColumn().isUnique(),
					false //We do copy no strategy here
			);
			linkWithValue( value );
		}
}
