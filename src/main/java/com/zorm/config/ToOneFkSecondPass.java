package com.zorm.config;

import java.util.Iterator;
import java.util.Map;

import com.zorm.config.annotations.TableBinder;
import com.zorm.exception.AnnotationException;
import com.zorm.exception.MappingException;
import com.zorm.mapping.KeyValue;
import com.zorm.mapping.ManyToOne;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Property;
import com.zorm.mapping.ToOne;
import com.zorm.util.StringHelper;

public class ToOneFkSecondPass extends FkSecondPass {

	private boolean unique;
	private Mappings mappings;
	private String path;
	private String entityClassName;

	public ToOneFkSecondPass(
			ToOne value,
			Ejb3JoinColumn[] columns,
			boolean unique,
			String entityClassName,
			String path,
			Mappings mappings) {
		super( value, columns );
		this.mappings = mappings;
		this.unique = unique;
		this.entityClassName = entityClassName;
		this.path = entityClassName != null ? path.substring( entityClassName.length() + 1 ) : path;
	}
	
	@Override
    public String getReferencedEntityName() {
		return ( (ToOne) value ).getReferencedEntityName();
	}

	@Override
    public boolean isInPrimaryKey() {
		if ( entityClassName == null ) return false;
		final PersistentClass persistentClass = mappings.getClass( entityClassName );
		Property property = persistentClass.getIdentifierProperty();
		if ( path == null ) {
			return false;
		}
		else if ( property != null) {
			//try explicit identifier property
			return path.startsWith( property.getName() + "." );
		}
		else {
			if ( path.startsWith( "id." ) ) {
				KeyValue valueIdentifier = persistentClass.getIdentifier();
				String localPath = path.substring( 3 );
//				if ( valueIdentifier instanceof Component ) {
//					Iterator it = ( (Component) valueIdentifier ).getPropertyIterator();
//					while ( it.hasNext() ) {
//						Property idProperty = (Property) it.next();
//						if ( localPath.startsWith( idProperty.getName() ) ) return true;
//					}
//
//				}
			}
		}
		return false;
	}

	public void doSecondPass(java.util.Map persistentClasses) throws MappingException {
		if ( value instanceof ManyToOne ) {
			ManyToOne manyToOne = (ManyToOne) value;
			PersistentClass ref = (PersistentClass) persistentClasses.get( manyToOne.getReferencedEntityName() );
			if ( ref == null ) {
				throw new AnnotationException(
						"@OneToOne or @ManyToOne on "
								+ StringHelper.qualify( entityClassName, path )
								+ " references an unknown entity: "
								+ manyToOne.getReferencedEntityName()
				);
			}
			BinderHelper.createSyntheticPropertyReference( columns, ref, null, manyToOne, false, mappings );
			TableBinder.bindFk( ref, null, columns, manyToOne, unique, mappings );
			
			if ( !manyToOne.isIgnoreNotFound() )
				manyToOne.createPropertyRefConstraints( persistentClasses );
		}
//		else if ( value instanceof OneToOne ) {
//			( (OneToOne) value ).createForeignKey();
//		}
//		else {
//			throw new AssertionFailure( "FkSecondPass for a wrong value type: " + value.getClass().getName() );
//		}
	}

}
