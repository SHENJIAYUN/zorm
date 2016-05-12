package com.zorm.config;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.config.annotations.Nullability;
import com.zorm.exception.AnnotationException;
import com.zorm.exception.AssertionFailure;
import com.zorm.mapping.Column;
import com.zorm.mapping.Join;
import com.zorm.mapping.SimpleValue;
import com.zorm.mapping.Table;
import com.zorm.util.StringHelper;

public class Ejb3Column {

	private static final Log log = LogFactory.getLog(Ejb3Column.class);
	private Column mappingColumn;
	private boolean insertable = true;
	private boolean updatable = true;
	private String secondaryTableName;
	protected Map<String, Join> joins;
	protected PropertyHolder propertyHolder;
	private Mappings mappings;
	private boolean isImplicit;
	public static final int DEFAULT_COLUMN_LENGTH = 255;
	public String sqlType;
	private int length = DEFAULT_COLUMN_LENGTH;
	private int precision;
	private int scale;
	private String logicalColumnName;
	private String propertyName;
	private boolean unique;
	private boolean nullable = true;
	private Table table;

	public Ejb3Column() {
	}

	protected Mappings getMappings() {
		return mappings;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public static Ejb3Column[] buildColumnFromAnnotation(
			javax.persistence.Column[] anns, Nullability nullability,
			PropertyHolder propertyHolder, PropertyData inferredData,
			Mappings mappings) {
		return buildColumnFromAnnotation(anns, nullability, propertyHolder,
				inferredData, null, mappings);
	}

	/*
	 * 从程序注解中获取数据库列的相关信息
	 */
	public static Ejb3Column[] buildColumnFromAnnotation(
			javax.persistence.Column[] anns, Nullability nullability,
			PropertyHolder propertyHolder, PropertyData inferredData,
			String suffixForDefaultColumnName, Mappings mappings) {
		Ejb3Column[] columns;
		javax.persistence.Column[] actualCols = anns;
		javax.persistence.Column[] overriddenCols = propertyHolder
				.getOverriddenColumn(StringHelper.qualify(
						propertyHolder.getPath(),
						inferredData.getPropertyName()));

		if (overriddenCols != null) {
			if (anns != null && overriddenCols.length != anns.length) {
				throw new AnnotationException(
						"AttributeOverride.column() should override all columns for now");
			}
			actualCols = overriddenCols.length == 0 ? null : overriddenCols;
		}
		if (actualCols == null) {
			columns = buildImplicitColumn(inferredData,
					suffixForDefaultColumnName, propertyHolder, nullability,
					mappings);
		} else {
			final int length = actualCols.length;
			columns = new Ejb3Column[length];
			for (int index = 0; index < length; index++) {
				final ObjectNameNormalizer nameNormalizer = mappings.getObjectNameNormalizer();
				javax.persistence.Column col = actualCols[index];
				final String sqlType = col.columnDefinition().equals("") ? null
						: nameNormalizer.normalizeIdentifierQuoting(col
								.columnDefinition());
				final String tableName = !StringHelper.isEmpty(col.table()) ? nameNormalizer
						.normalizeIdentifierQuoting(mappings
								.getNamingStrategy().tableName(col.table()))
						: "";
				final String columnName = nameNormalizer
						.normalizeIdentifierQuoting(col.name());
				Ejb3Column column = new Ejb3Column();
				column.setImplicit(false);
				column.setSqlType(sqlType);
				column.setLength(col.length());
				column.setPrecision(col.precision());
				column.setScale(col.scale());
				if (StringHelper.isEmpty(columnName)
						&& !StringHelper.isEmpty(suffixForDefaultColumnName)) {
					column.setLogicalColumnName(inferredData.getPropertyName()
							+ suffixForDefaultColumnName);
				} else {
					column.setLogicalColumnName(columnName);
				}

				column.setPropertyName(BinderHelper.getRelativePath(
						propertyHolder, inferredData.getPropertyName()));
				column.setNullable(col.nullable());
				column.setUnique(col.unique());
				column.setInsertable(col.insertable());
				column.setUpdatable(col.updatable());
				column.setSecondaryTableName(tableName);
				column.setPropertyHolder(propertyHolder);
				// column.setJoins( secondaryTables );
				column.setMappings(mappings);
				column.bind();
				columns[index] = column;
			}
		}
		return columns;
	}

	public void setMappings(Mappings mappings) {
		this.mappings = mappings;
	}

	public void setSecondaryTableName(String tableName) {
		this.secondaryTableName = tableName;
	}

	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public boolean isImplicit() {
		return isImplicit;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

	public void setSqlType(String sqlType) {
		this.sqlType = sqlType;
	}

	public void setLogicalColumnName(String logicalColumnName) {
		this.logicalColumnName = logicalColumnName;
	}

	public String getLogicalColumnName() {
		return logicalColumnName;
	}

	private static Ejb3Column[] buildImplicitColumn(PropertyData inferredData,
			String suffixForDefaultColumnName, PropertyHolder propertyHolder,
			Nullability nullability, Mappings mappings) {
		Ejb3Column column = new Ejb3Column();
		Ejb3Column[] columns = new Ejb3Column[1];
		columns[0] = column;
		if (nullability != Nullability.FORCED_NULL
				&& inferredData.getClassOrElement().isPrimitive()
				&& !inferredData.getProperty().isArray()) {
			column.setNullable(false);
		}
		column.setLength(DEFAULT_COLUMN_LENGTH);
		final String propertyName = inferredData.getPropertyName();
		column.setPropertyName(BinderHelper.getRelativePath(propertyHolder,
				propertyName));
		column.setPropertyHolder(propertyHolder);
		column.setMappings(mappings);

		// property name + suffix is an "explicit" column name
		if (!StringHelper.isEmpty(suffixForDefaultColumnName)) {
			column.setLogicalColumnName(propertyName
					+ suffixForDefaultColumnName);
			column.setImplicit(false);
		} else {
			column.setImplicit(true);
		}
		column.bind();
		return columns;
	}

	public void bind() {
		initMappingColumn(logicalColumnName, propertyName, length, precision,
				scale, nullable, sqlType, unique, true);
		log.debug("Binding column:" + toString());
	}

	protected void initMappingColumn(String logicalColumnName,
			String propertyName, int length, int precision, int scale,
			boolean nullable, String sqlType, boolean unique,
			boolean applyNamingStrategy) {
		this.mappingColumn = new Column();
		//设置列名
		redefineColumnName(logicalColumnName, propertyName, applyNamingStrategy);
		this.mappingColumn.setLength(length);
		if (precision > 0) {
			this.mappingColumn.setPrecision(precision);
			this.mappingColumn.setScale(scale);
		}
		this.mappingColumn.setNullable(nullable);
		this.mappingColumn.setSqlType(sqlType);
		this.mappingColumn.setUnique(unique);
	}

	public void redefineColumnName(String columnName, String propertyName,
			boolean applyNamingStrategy) {
		if (applyNamingStrategy) {
			if (StringHelper.isEmpty(columnName)) {
				if (propertyName != null) {
					mappingColumn
							.setName(mappings.getObjectNameNormalizer()
									.normalizeIdentifierQuoting(
											mappings.getNamingStrategy()
													.propertyToColumnName(
															propertyName)));
				}
			} else {
				columnName = mappings.getObjectNameNormalizer()
						.normalizeIdentifierQuoting(columnName);
				columnName = mappings.getNamingStrategy()
						.columnName(columnName);
				columnName = mappings.getObjectNameNormalizer()
						.normalizeIdentifierQuoting(columnName);
				mappingColumn.setName(columnName);
			}
		} else {
			if (StringHelper.isNotEmpty(columnName)) {
				mappingColumn.setName(mappings.getObjectNameNormalizer()
						.normalizeIdentifierQuoting(columnName));
			}
		}
	}

	public void setImplicit(boolean implicit) {
		isImplicit = implicit;
	}

	public void setPropertyHolder(PropertyHolder propertyHolder) {
		this.propertyHolder = propertyHolder;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public void setNullable(boolean nullable) {
		if (mappingColumn != null) {
			mappingColumn.setNullable(nullable);
		} else {
			this.nullable = nullable;
		}
	}

	public void forceNotNull() {
		mappingColumn.setNullable(false);
	}

	public boolean isInsertable() {
		return insertable;
	}

	public boolean isUpdateable() {
		return updatable;
	}

	public Table getTable() {
		if (table != null)
			return table;
		return propertyHolder.getTable();
	}

	public boolean isNameDeferred() {
		return mappingColumn == null
				|| StringHelper.isEmpty(mappingColumn.getName());
	}

	public void linkWithValue(SimpleValue value) {
		getMappingColumn().setValue(value);
		value.addColumn(getMappingColumn());
		value.getTable().addColumn(getMappingColumn());
		addColumnBinding(value);
		table = value.getTable();
	}

	protected void addColumnBinding(SimpleValue value) {
		String logicalColumnName = mappings.getNamingStrategy()
				.logicalColumnName(this.logicalColumnName, propertyName);
		mappings.addColumnBinding(logicalColumnName, getMappingColumn(),
				value.getTable());
	}

	public Column getMappingColumn() {
		return mappingColumn;
	}

	public boolean isSecondary() {
		if (propertyHolder == null) {
			throw new AssertionFailure(
					"Should not call getTable() on column wo persistent class defined");
		}
		if (StringHelper.isNotEmpty(secondaryTableName)) {
			return true;
		}
		// else {
		return false;
	}

	public void setJoins(Map<String, Join> joins) {
		this.joins = joins;
	}

	public PropertyHolder getPropertyHolder() {
		return propertyHolder;
	}

	public boolean isUpdatable() {
		return updatable;
	}

	public static void checkPropertyConsistency(Ejb3Column[] columns,
			String propertyName) {
		int nbrOfColumns = columns.length;

		if (nbrOfColumns > 1) {
			for (int currentIndex = 1; currentIndex < nbrOfColumns; currentIndex++) {

				if (columns[currentIndex].isInsertable() != columns[currentIndex - 1]
						.isInsertable()) {
					throw new AnnotationException(
							"Mixing insertable and non insertable columns in a property is not allowed: "
									+ propertyName);
				}
				if (columns[currentIndex].isNullable() != columns[currentIndex - 1]
						.isNullable()) {
					throw new AnnotationException(
							"Mixing nullable and non nullable columns in a property is not allowed: "
									+ propertyName);
				}
				if (columns[currentIndex].isUpdatable() != columns[currentIndex - 1]
						.isUpdatable()) {
					throw new AnnotationException(
							"Mixing updatable and non updatable columns in a property is not allowed: "
									+ propertyName);
				}
				if (!columns[currentIndex].getTable().equals(
						columns[currentIndex - 1].getTable())) {
					throw new AnnotationException(
							"Mixing different tables in a property is not allowed: "
									+ propertyName);
				}
			}
		}

	}

	public boolean isNullable() {
		return mappingColumn.isNullable();
	}

	public void setTable(Table collectionTable) {
		this.table = collectionTable;
	}
}
