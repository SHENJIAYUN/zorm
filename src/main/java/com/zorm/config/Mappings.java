package com.zorm.config;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.zorm.annotations.reflection.ReflectionManager;
import com.zorm.annotations.reflection.XClass;
import com.zorm.exception.DuplicateMappingException;
import com.zorm.mapping.Collection;
import com.zorm.mapping.Column;
import com.zorm.mapping.Join;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Table;
import com.zorm.type.TypeDef;
import com.zorm.type.TypeResolver;

public interface Mappings {
	/**
	 * Retrieve the type resolver in effect.
	 *
	 * @return The type resolver.
	 */
	public TypeResolver getTypeResolver();

	/**
	 * Get and maintain a cache of class type.
	 *
	 * @param clazz The XClass mapping
	 *
	 * @return The class type.
	 */
	public AnnotatedClassType getClassType(XClass clazz);

	public PersistentClass getClass(String entityName);

	public ObjectNameNormalizer getObjectNameNormalizer();

	public NamingStrategy getNamingStrategy();

	public void addImport(String entityName, String rename) throws DuplicateMappingException;

	public String getSchemaName();

	public String getCatalogName();

	public Table addDenormalizedTable(String schema, String catalog,
			String realTableName, Boolean isAbstract, String subselect,
			Table denormalizedSuperclassTable) throws DuplicateMappingException;

	public Table addTable(String schema, String catalog, String realTableName,
			String subselect, Boolean isAbstract);

	public void addUniqueConstraintHolders(Table table,
			List<UniqueConstraintHolder> uniqueConstraints);

	public void addTableBinding(String schema, String catalog,
			String logicalName, String realTableName,
			Table denormalizedSuperTable)throws DuplicateMappingException;

	public ReflectionManager getReflectionManager();

	public boolean useNewGeneratorMappings();

	public void addClass(PersistentClass persistentClass) throws DuplicateMappingException;

	public boolean forceDiscriminatorInSelectsByDefault();

	public boolean isSpecjProprietarySyntaxEnabled();

	public void addPropertyAnnotatedWithMapsIdSpecj(XClass entity,
			PropertyData specJPropertyData, String string);

	public void addToOneAndIdProperty(XClass entity,
			PropertyData propertyAnnotatedElement);

	public void addPropertyAnnotatedWithMapsId(XClass entity,
			PropertyData propertyAnnotatedElement);

	public com.zorm.mapping.MappedSuperclass getMappedSuperclass(Class<?> type);

	public void addMappedSuperclass(Class<?> type,
			com.zorm.mapping.MappedSuperclass mappedSuperclass);

	public PropertyData getPropertyAnnotatedWithIdAndToOne(
			XClass persistentXClass, String propertyName);

	public PropertyData getPropertyAnnotatedWithMapsId(XClass persistentXClass,
			String propertyName);

	public void addColumnBinding(String logicalColumnName,
			Column mappingColumn, Table table) throws DuplicateMappingException;

	public boolean isInSecondPass();

	public Properties getConfigurationProperties();

	public void addSecondPass(SecondPass secondPass);

	public TypeDef getTypeDef(String typeName);
	
	public static final class PropertyReference implements Serializable {
		public final String referencedClass;
		public final String propertyName;
		public final boolean unique;

		public PropertyReference(String referencedClass, String propertyName, boolean unique) {
			this.referencedClass = referencedClass;
			this.propertyName = propertyName;
			this.unique = unique;
		}
	}

	public String getLogicalColumnName(String quotedName, Table table);

	public String getPhysicalColumnName(String logicalName, Table table);

	public String getLogicalTableName(Table table);

	public void addJoins(PersistentClass persistentClass,
			Map<String, Join> secondaryTables);

	public Map getClasses();

	public void addMappedBy(String name, String mappedBy, String propertyName);

	public Map<String, Join> getJoins(String entityName);

	public String getPropertyReferencedAssociation(String entityName,
			String mappedBy);

	public void addPropertyReference(String ownerEntityName, String propRef);

	public String getFromMappedBy(String ownerEntityName, String propertyName);

	public void addUniquePropertyReference(String entityName,
			String referencedPropertyName);

	public void addSecondPass(SecondPass sp, boolean onTopOfTheQueue);

	public void addCollection(Collection collection);
}
