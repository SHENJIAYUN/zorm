package com.zorm.config;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.annotations.reflection.XAnnotatedElement;
import com.zorm.annotations.reflection.XClass;
import com.zorm.config.annotations.TableBinder;
import com.zorm.exception.AnnotationException;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.MappingException;
import com.zorm.id.PersistentIdentifierGenerator;
import com.zorm.mapping.Column;
import com.zorm.mapping.IdGenerator;
import com.zorm.mapping.Join;
import com.zorm.mapping.MappedSuperclass;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Property;
import com.zorm.mapping.SimpleValue;
import com.zorm.mapping.Table;
import com.zorm.mapping.ToOne;
import com.zorm.mapping.Value;
import com.zorm.util.StringHelper;


/**
 * @author Emmanuel Bernard
 */
public class BinderHelper {

	public static final String ANNOTATION_STRING_DEFAULT = "";

    private static final Log log = LogFactory.getLog(BinderHelper.class);
	
	private BinderHelper() {
	}

	static {
		Set<String> primitiveNames = new HashSet<String>();
		primitiveNames.add( byte.class.getName() );
		primitiveNames.add( short.class.getName() );
		primitiveNames.add( int.class.getName() );
		primitiveNames.add( long.class.getName() );
		primitiveNames.add( float.class.getName() );
		primitiveNames.add( double.class.getName() );
		primitiveNames.add( char.class.getName() );
		primitiveNames.add( boolean.class.getName() );
		PRIMITIVE_NAMES = Collections.unmodifiableSet( primitiveNames );
	}

	public static final Set<String> PRIMITIVE_NAMES;

	public static String getRelativePath(PropertyHolder propertyHolder, String propertyName) {
		if ( propertyHolder == null ) return propertyName;
		String path = propertyHolder.getPath();
		String entityName = propertyHolder.getPersistentClass().getEntityName();
		if ( path.length() == entityName.length() ) {
			return propertyName;
		}
		else {
			return StringHelper.qualify( path.substring( entityName.length() + 1 ), propertyName );
		}
	}
	/**
	 * apply an id generator to a SimpleValue
	 */
	public static void makeIdGenerator(
			SimpleValue id,
			String generatorType,
			String generatorName,
			Mappings mappings,
			Map<String, IdGenerator> localGenerators) {
		Table table = id.getTable();
		table.setIdentifierValue( id );
		id.setIdentifierGeneratorStrategy( generatorType );
		Properties params = new Properties();
		params.setProperty(
				PersistentIdentifierGenerator.TABLE, table.getName()
		);

		if ( id.getColumnSpan() == 1 ) {
			params.setProperty(
					PersistentIdentifierGenerator.PK,
					( (Column) id.getColumnIterator().next() ).getName()
			);
		}
		params.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER, mappings.getObjectNameNormalizer() );

		if ( "assigned".equals( generatorType ) ) id.setNullValue( "undefined" );
		id.setIdentifierGeneratorProperties( params );
	}

	public static boolean isEmptyAnnotationValue(String annotationString) {
		return annotationString != null && annotationString.length() == 0;
	}


	public static MappedSuperclass getMappedSuperclassOrNull(
			XClass declaringClass,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			Mappings mappings) {
		boolean retrieve = false;
		if ( declaringClass != null ) {
			final InheritanceState inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState.isEmbeddableSuperclass() ) {
				retrieve = true;
			}
		}
		return retrieve ?
				mappings.getMappedSuperclass( mappings.getReflectionManager().toClass( declaringClass ) ) :
		        null;
	}

	public static String getPath(PropertyHolder holder, PropertyData property) {
		return StringHelper.qualify( holder.getPath(), property.getPropertyName() );
	}

	static PropertyData getPropertyOverriddenByMapperOrMapsId(
			boolean isId,
			PropertyHolder propertyHolder,
			String propertyName,
			Mappings mappings) {
		final XClass persistentXClass;
		try {
			 persistentXClass = mappings.getReflectionManager()
					.classForName( propertyHolder.getPersistentClass().getClassName(), AnnotationBinder.class );
		}
		catch ( ClassNotFoundException e ) {
			throw new AssertionFailure( "PersistentClass name cannot be converted into a Class", e);
		}
		if ( propertyHolder.isInIdClass() ) {
			PropertyData pd = mappings.getPropertyAnnotatedWithIdAndToOne( persistentXClass, propertyName );
			if ( pd == null && mappings.isSpecjProprietarySyntaxEnabled() ) {
				pd = mappings.getPropertyAnnotatedWithMapsId( persistentXClass, propertyName );
			}
			return pd;
		}
        String propertyPath = isId ? "" : propertyName;
        return mappings.getPropertyAnnotatedWithMapsId(persistentXClass, propertyPath);
	}
	
	public static Object findColumnOwner(
			PersistentClass persistentClass,
			String columnName,
			Mappings mappings) {
		if ( StringHelper.isEmpty( columnName ) ) {
			return persistentClass; //shortcut for implicit referenced column names
		}
		PersistentClass current = persistentClass;
		Object result;
		boolean found = false;
		do {
			result = current;
			Table currentTable = current.getTable();
			try {
				mappings.getPhysicalColumnName( columnName, currentTable );
				found = true;
			}
			catch (MappingException me) {
				//swallow it
			}
			Iterator joins = current.getJoinIterator();
			while ( !found && joins.hasNext() ) {
				result = joins.next();
				currentTable = ( (Join) result ).getTable();
				try {
					mappings.getPhysicalColumnName( columnName, currentTable );
					found = true;
				}
				catch (MappingException me) {
					//swallow it
				}
			}
			current = current.getSuperclass();
		}
		while ( !found && current != null );
		return found ? result : null;
	}
	
	public static void createSyntheticPropertyReference(
			Ejb3JoinColumn[] columns,
			PersistentClass ownerEntity,
			PersistentClass associatedEntity,
			Value value,
			boolean inverse,
			Mappings mappings) {
		if ( columns[0].isImplicit() || StringHelper.isNotEmpty( columns[0].getMappedBy() ) ) return;
		int fkEnum = Ejb3JoinColumn.checkReferencedColumnsType( columns, ownerEntity, mappings );
		PersistentClass associatedClass = columns[0].getPropertyHolder() != null ?
				columns[0].getPropertyHolder().getPersistentClass() :
				null;
		if(Ejb3JoinColumn.NON_PK_REFERENCE == fkEnum){
			
		}
	}
}
