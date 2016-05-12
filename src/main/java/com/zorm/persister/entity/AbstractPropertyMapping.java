package com.zorm.persister.entity;

import java.util.*;

import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.exception.QueryException;
import com.zorm.type.AssociationType;
import com.zorm.type.EntityType;
import com.zorm.type.Type;
import com.zorm.util.StringHelper;

public abstract class AbstractPropertyMapping implements PropertyMapping{
	private final Map typesByPropertyPath = new HashMap();
	private final Map columnsByPropertyPath = new HashMap();
	private final Map columnReadersByPropertyPath = new HashMap();
	private final Map columnReaderTemplatesByPropertyPath = new HashMap();
	private final Map formulaTemplatesByPropertyPath = new HashMap();

	public String[] getIdentifierColumnNames() {
		throw new UnsupportedOperationException("one-to-one is not supported here");
	}

	public String[] getIdentifierColumnReaderTemplates() {
		throw new UnsupportedOperationException("one-to-one is not supported here");
	}

	public String[] getIdentifierColumnReaders() {
		throw new UnsupportedOperationException("one-to-one is not supported here");
	}

	protected abstract String getEntityName();

	public Type toType(String propertyName) throws QueryException {
		Type type = (Type) typesByPropertyPath.get(propertyName);
		if ( type == null ) {
			throw propertyException( propertyName );
		}
		return type;
	}

	protected final QueryException propertyException(String propertyName) throws QueryException {
		return new QueryException( "could not resolve property: " + propertyName + " of: " + getEntityName() );
	}

	public String[] getColumnNames(String propertyName) {
		String[] cols = (String[]) columnsByPropertyPath.get(propertyName);
		if (cols==null) {
			throw new MappingException("unknown property: " + propertyName);
		}
		return cols;
	}

	public String[] toColumns(String alias, String propertyName) throws QueryException {
		String[] columns = (String[]) columnsByPropertyPath.get(propertyName);
		if ( columns == null ) {
			throw propertyException( propertyName );
		}
		String[] formulaTemplates = (String[]) formulaTemplatesByPropertyPath.get(propertyName);
		String[] columnReaderTemplates = (String[]) columnReaderTemplatesByPropertyPath.get(propertyName);
		String[] result = new String[columns.length];
		for ( int i=0; i<columns.length; i++ ) {
			if ( columnReaderTemplates[i]==null ) {
				result[i] = StringHelper.replace( formulaTemplates[i], Template.TEMPLATE, alias );
			}
			else {
				result[i] = StringHelper.replace( columnReaderTemplates[i], Template.TEMPLATE, alias );
			}
		}
		return result;
	}

	public String[] toColumns(String propertyName) throws QueryException {
		String[] columns = (String[]) columnsByPropertyPath.get(propertyName);
		if ( columns == null ) {
			throw propertyException( propertyName );
		}
		String[] formulaTemplates = (String[]) formulaTemplatesByPropertyPath.get(propertyName);
		String[] columnReaders = (String[]) columnReadersByPropertyPath.get(propertyName);
		String[] result = new String[columns.length];
		for ( int i=0; i<columns.length; i++ ) {
			if ( columnReaders[i]==null ) {
				result[i] = StringHelper.replace( formulaTemplates[i], Template.TEMPLATE, "" );
			}
			else {
				result[i] = columnReaders[i];
			}
		}
		return result;
	}

	protected void addPropertyPath(
			String path,
			Type type,
			String[] columns,
			String[] columnReaders,
			String[] columnReaderTemplates,
			String[] formulaTemplates) {
		if ( typesByPropertyPath.containsKey( path ) ) {
			return;
		}
		typesByPropertyPath.put(path, type);
		columnsByPropertyPath.put(path, columns);
		columnReadersByPropertyPath.put(path, columnReaders);
		columnReaderTemplatesByPropertyPath.put(path, columnReaderTemplates);
		if (formulaTemplates!=null) {
			formulaTemplatesByPropertyPath.put(path, formulaTemplates);
		}
	}

	/*protected void initPropertyPaths(
			final String path,
			final Type type,
			final String[] columns,
			final String[] formulaTemplates,
			final Mapping factory)
	throws MappingException {
		//addFormulaPropertyPath(path, type, formulaTemplates);
		initPropertyPaths(path, type, columns, formulaTemplates, factory);
	}*/

	protected void initPropertyPaths(
			final String path,
			final Type type,
			String[] columns,
			String[] columnReaders,
			String[] columnReaderTemplates,
			final String[] formulaTemplates,
			final Mapping factory)
	throws MappingException {

		if ( columns.length!=type.getColumnSpan(factory) ) {
			throw new MappingException(
					"broken column mapping for: " + path +
					" of: " + getEntityName()
				);
		}

		if ( type.isAssociationType() ) {
//			AssociationType actype = (AssociationType) type;
//			if ( actype.useLHSPrimaryKey() ) {
//				columns = getIdentifierColumnNames();
//				columnReaders = getIdentifierColumnReaders();
//				columnReaderTemplates = getIdentifierColumnReaderTemplates();
//			}
//			else {
//				String foreignKeyProperty = actype.getLHSPropertyName();
//				if ( foreignKeyProperty!=null && !path.equals(foreignKeyProperty) ) {
//					//TODO: this requires that the collection is defined after the
//					//      referenced property in the mapping file (ok?)
//					columns = (String[]) columnsByPropertyPath.get(foreignKeyProperty);
//					if (columns==null) return; //get em on the second pass!
//					columnReaders = (String[]) columnReadersByPropertyPath.get(foreignKeyProperty);
//					columnReaderTemplates = (String[]) columnReaderTemplatesByPropertyPath.get(foreignKeyProperty);
//				}
//			}
		}

		if (path!=null) 
			addPropertyPath(path, type, columns, columnReaders, columnReaderTemplates, formulaTemplates);

		if ( type.isComponentType() ) {
		}
		else if ( type.isEntityType() ) {
			initIdentifierPropertyPaths( path, (EntityType) type, columns, columnReaders, columnReaderTemplates, factory );
		}
	}

	protected void initIdentifierPropertyPaths(
			final String path,
			final EntityType etype,
			final String[] columns,
			final String[] columnReaders,
			final String[] columnReaderTemplates,
			final Mapping factory) throws MappingException {

//		Type idtype = etype.getIdentifierOrUniqueKeyType( factory );
//		String idPropName = etype.getIdentifierOrUniqueKeyPropertyName(factory);
//		boolean hasNonIdentifierPropertyNamedId = hasNonIdentifierPropertyNamedId( etype, factory );
//
//		if ( etype.isReferenceToPrimaryKey() ) {
//			if ( !hasNonIdentifierPropertyNamedId ) {
//				String idpath1 = extendPath(path, EntityPersister.ENTITY_ID);
//				addPropertyPath(idpath1, idtype, columns, columnReaders, columnReaderTemplates, null);
//				initPropertyPaths(idpath1, idtype, columns, columnReaders, columnReaderTemplates, null, factory);
//			}
//		}
//
//		if (idPropName!=null) {
//			String idpath2 = extendPath(path, idPropName);
//			addPropertyPath(idpath2, idtype, columns, columnReaders, columnReaderTemplates, null);
//			initPropertyPaths(idpath2, idtype, columns, columnReaders, columnReaderTemplates, null, factory);
//		}
	}

	private boolean hasNonIdentifierPropertyNamedId(final EntityType entityType, final Mapping factory) {
		// TODO : would be great to have a Mapping#hasNonIdentifierPropertyNamedId method
		// I don't believe that Mapping#getReferencedPropertyType accounts for the identifier property; so
		// if it returns for a property named 'id', then we should have a non-id field named id
//		try {
//			return factory.getReferencedPropertyType( entityType.getAssociatedEntityName(), EntityPersister.ENTITY_ID ) != null;
//		}
//		catch( MappingException e ) {
//			return false;
//		}
		return false;
	}


	private static String extendPath(String path, String property) {
		return StringHelper.isEmpty( path ) ? property : StringHelper.qualify( path, property );
	}
}
