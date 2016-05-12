package com.zorm.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.zorm.FetchMode;
import com.zorm.config.Environment;
import com.zorm.config.Mappings;
import com.zorm.dialect.Dialect;
import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.id.IdentifierGenerator;
import com.zorm.id.IdentifierGeneratorFactory;
import com.zorm.id.PersistentIdentifierGenerator;
import com.zorm.type.Type;

public  class SimpleValue implements KeyValue{
	public static final String DEFAULT_ID_GEN_STRATEGY = "assigned";
	
	private final Mappings mappings;

	private final List columns = new ArrayList();
	private String typeName;
	private Properties identifierGeneratorProperties;
	private String identifierGeneratorStrategy = DEFAULT_ID_GEN_STRATEGY;
	private String nullValue;
	private Table table;
	private String foreignKeyName;
	private boolean alternateUniqueKey;
	private Properties typeParameters;
	private boolean cascadeDeleteEnabled;

	public SimpleValue(Mappings mappings) {
		this.mappings = mappings;
	}

	public SimpleValue(Mappings mappings, Table table) {
		this( mappings );
		this.table = table;
	}
	
	public String getForeignKeyName() {
		return foreignKeyName;
	}

	public void setForeignKeyName(String foreignKeyName) {
		this.foreignKeyName = foreignKeyName;
	}
	
	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}
	
	public void createForeignKeyOfEntity(String entityName) {
		if ( !hasFormula() && !"none".equals(getForeignKeyName())) {
			ForeignKey fk = table.createForeignKey( getForeignKeyName(), getConstraintColumns(), entityName );
			fk.setCascadeDeleteEnabled(cascadeDeleteEnabled);
		}
	}
	
	public List getConstraintColumns() {
		return columns;
	}
	
	public String getTypeName() {
		return typeName;
	}
	public void setTypeName(String type) {
		this.typeName = type;
	}
	
	public void setCascadeDeleteEnabled(boolean cascadeDeleteEnabled) {
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
	}

	@Override
	public int getColumnSpan() {
		return columns.size();
	}

	@Override
	public Iterator getColumnIterator() {
		return columns.iterator();
	}

	@Override
	public Type getType() throws MappingException {
        if(typeName == null){
        	throw new MappingException("No type name");
        }
    	Type result = mappings.getTypeResolver().heuristicType( typeName, typeParameters );
    	if ( result == null ) {
			String msg = "Could not determine type for: " + typeName;
			if ( table != null ) {
				msg += ", at table: " + table.getName();
			}
			if ( columns != null && columns.size() > 0 ) {
				msg += ", for columns: " + columns;
			}
			throw new MappingException( msg );
		}
		return result;
	}

	@Override
	public FetchMode getFetchMode() {
		return FetchMode.SELECT;
	}

	@Override
	public Table getTable() {
		return table;
	}

	@Override
	public boolean hasFormula() {
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			Object o = iter.next();
//			if (o instanceof Formula) return true;
		}
		return false;
	}

	@Override
	public boolean isNullable() {
		if ( hasFormula() ) return true;
		boolean nullable = true;
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			if ( !( (Column) iter.next() ).isNullable() ) {
				nullable = false;
				return nullable; //shortcut
			}
		}
		return nullable;
	}

	@Override
	public boolean[] getColumnUpdateability() {
		return getColumnInsertability();
	}

	@Override
	public boolean[] getColumnInsertability() {
		boolean[] result = new boolean[ getColumnSpan() ];
		int i = 0;
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			Selectable s = (Selectable) iter.next();
			result[i++] = !s.isFormula();
		}
		return result;
	}

	@Override
	public void createForeignKey() throws MappingException {
	}

	@Override
	public boolean isSimpleValue() {
		return false;
	}

	@Override
	public boolean isValid(Mapping mapping) throws MappingException {
		return getColumnSpan()==getType().getColumnSpan(mapping);
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName)
			throws MappingException {
		
	}

	@Override
	public IdentifierGenerator createIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect, 
			String defaultCatalog, 
			String defaultSchema,
			RootClass rootClass) throws MappingException {
 
		//params:存储的是类和数据库表的映射信息
		Properties params = new Properties();
		if(defaultSchema!=null){
			params.setProperty(PersistentIdentifierGenerator.SCHEMA, defaultSchema);
		}
		if(defaultCatalog!=null){
			params.setProperty(PersistentIdentifierGenerator.CATALOG, defaultCatalog);
		}
		
		if(rootClass != null){
			params.setProperty(IdentifierGenerator.ENTITY_NAME, rootClass.getEntityName());
			params.setProperty(IdentifierGenerator.JPA_ENTITY_NAME, rootClass.getJpaEntityName());
		}
		String tableName = getTable().getQuotedName(dialect);
		params.setProperty(PersistentIdentifierGenerator.TABLE, tableName);
		
		String columnName = ( (Column) getColumnIterator().next() ).getQuotedName(dialect);
		params.setProperty( PersistentIdentifierGenerator.PK, columnName );
		
		if(rootClass!=null){
			StringBuilder tables = new StringBuilder();
			Iterator iter = rootClass.getIdentityTables().iterator();
			while(iter.hasNext()){
				Table table = (Table) iter.next();
				tables.append(table.getQuotedName(dialect));
				if(iter.hasNext()) tables.append(", ");
			}
			params.setProperty(PersistentIdentifierGenerator.TABLES, tables.toString());
		}
		else{
			params.setProperty(PersistentIdentifierGenerator.TABLES, tableName);
		}
		
		if (identifierGeneratorProperties!=null) {
			params.putAll(identifierGeneratorProperties);
		}
		
		params.put(
				Environment.PREFER_POOLED_VALUES_LO,
				mappings.getConfigurationProperties().getProperty( Environment.PREFER_POOLED_VALUES_LO, "false" )
		);
		identifierGeneratorFactory.setDialect( dialect );
		return identifierGeneratorFactory.createIdentifierGenerator( identifierGeneratorStrategy, getType(), params );
	}

	@Override
	public boolean isIdentityColumn(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect) {
		return false;
	}

	@Override
	public String getNullValue() {
		return nullValue;
	}

	@Override
	public boolean isUpdateable() {
		return false;
	}

	public void addColumn(Column column) {
		if(!columns.contains(column)) {
			columns.add(column);
		}
		column.setValue(this);
		column.setTypeIndex(columns.size()-1);
	}

	public void setIdentifierGeneratorStrategy(String generatorType) {
		this.identifierGeneratorStrategy = generatorType;
	}

	public void setNullValue(String nullValue) {
		this.nullValue = nullValue;
	}

	public void setIdentifierGeneratorProperties(Properties params) {
		this.identifierGeneratorProperties = params;
	}

	public boolean isAlternateUniqueKey() {
		return alternateUniqueKey;
	}

	public void setTypeParameters(Properties parameters) {
		this.typeParameters = parameters;
	}

	public boolean isTypeSpecified() {
		return typeName!=null;
	}

	public Mappings getMappings() {
		return mappings;
	}

	public void setAlternateUniqueKey(boolean unique) {
		this.alternateUniqueKey = unique;
	}
}
