package com.zorm.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.Access;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;


import com.zorm.annotations.reflection.XClass;
import com.zorm.annotations.reflection.XProperty;
import com.zorm.exception.AnnotationException;
import com.zorm.exception.MappingException;
import com.zorm.util.StringHelper;

public class PropertyContainer {
	  static {
	        System.setProperty("jboss.i18n.generate-proxies", "true");
	    }

		private final AccessType explicitClassDefinedAccessType;

		private final TreeMap<String, XProperty> fieldAccessMap;

		private final TreeMap<String, XProperty> propertyAccessMap;

		private final XClass xClass;
		private final XClass entityAtStake;

		PropertyContainer(XClass clazz, XClass entityAtStake) {
			this.xClass = clazz;
			this.entityAtStake = entityAtStake;

			explicitClassDefinedAccessType = determineClassDefinedAccessStrategy();

			// 获取clazz的所有属性
			fieldAccessMap = initProperties( AccessType.FIELD );
			propertyAccessMap = initProperties( AccessType.PROPERTY );

		}

		public XClass getEntityAtStake() {
			return entityAtStake;
		}

		public XClass getDeclaringClass() {
			return xClass;
		}

		public AccessType getExplicitAccessStrategy() {
			return explicitClassDefinedAccessType;
		}

		public boolean hasExplicitAccessStrategy() {
			return !explicitClassDefinedAccessType.equals( AccessType.DEFAULT );
		}

		public Collection<XProperty> getProperties(AccessType accessType) {
			assertTypesAreResolvable( accessType );
			if ( AccessType.DEFAULT == accessType || AccessType.PROPERTY == accessType ) {
				return Collections.unmodifiableCollection( propertyAccessMap.values() );
			}
			else {
				return Collections.unmodifiableCollection( fieldAccessMap.values() );
			}
		}

		private void assertTypesAreResolvable(AccessType access) {
			Map<String, XProperty> xprops;
			if ( AccessType.PROPERTY.equals( access ) || AccessType.DEFAULT.equals( access ) ) {
				xprops = propertyAccessMap;
			}
			else {
				xprops = fieldAccessMap;
			}
			for ( XProperty property : xprops.values() ) {
				if ( !property.isTypeResolved() && !discoverTypeWithoutReflection( property ) ) {
					String msg = "Property " + StringHelper.qualify( xClass.getName(), property.getName() ) +
							" has an unbound type and no explicit target entity. Resolve this Generic usage issue" +
							" or set an explicit target attribute (eg @OneToMany(target=) or use an explicit @Type";
					throw new AnnotationException( msg );
				}
			}
		}

		private TreeMap<String, XProperty> initProperties(AccessType access) {
			if ( !( AccessType.PROPERTY.equals( access ) || AccessType.FIELD.equals( access ) ) ) {
				throw new IllegalArgumentException( "Access type has to be AccessType.FIELD or AccessType.Property" );
			}

			//order so that property are used in the same order when binding native query
			TreeMap<String, XProperty> propertiesMap = new TreeMap<String, XProperty>();
			List<XProperty> properties = xClass.getDeclaredProperties( access.getType() );
			for ( XProperty property : properties ) {
				if ( mustBeSkipped( property ) ) {
					continue;
				}
				propertiesMap.put( property.getName(), property );
			}
			return propertiesMap;
		}

		private AccessType determineClassDefinedAccessStrategy() {
			AccessType classDefinedAccessType;

			AccessType hibernateDefinedAccessType = AccessType.DEFAULT;
			AccessType jpaDefinedAccessType = AccessType.DEFAULT;

			Access access = xClass.getAnnotation( Access.class );
			if ( access != null ) {
				jpaDefinedAccessType = AccessType.getAccessStrategy( access.value() );
			}

			if ( hibernateDefinedAccessType != AccessType.DEFAULT
					&& jpaDefinedAccessType != AccessType.DEFAULT
					&& hibernateDefinedAccessType != jpaDefinedAccessType ) {
				throw new MappingException(
						"@AccessType and @Access specified with contradicting values. Use of @Access only is recommended. "
				);
			}

			if ( hibernateDefinedAccessType != AccessType.DEFAULT ) {
				classDefinedAccessType = hibernateDefinedAccessType;
			}
			else {
				classDefinedAccessType = jpaDefinedAccessType;
			}
			return classDefinedAccessType;
		}

		private static boolean discoverTypeWithoutReflection(XProperty p) {
			if ( p.isAnnotationPresent( OneToOne.class ) && !p.getAnnotation( OneToOne.class )
					.targetEntity()
					.equals( void.class ) ) {
				return true;
			}
			else if ( p.isAnnotationPresent( OneToMany.class ) && !p.getAnnotation( OneToMany.class )
					.targetEntity()
					.equals( void.class ) ) {
				return true;
			}
			else if ( p.isAnnotationPresent( ManyToOne.class ) && !p.getAnnotation( ManyToOne.class )
					.targetEntity()
					.equals( void.class ) ) {
				return true;
			}
			else if ( p.isAnnotationPresent( ManyToMany.class ) && !p.getAnnotation( ManyToMany.class )
					.targetEntity()
					.equals( void.class ) ) {
				return true;
			}
			return false;
		}

		private static boolean mustBeSkipped(XProperty property) {
			return property.isAnnotationPresent( Transient.class )
					|| "net.sf.cglib.transform.impl.InterceptFieldCallback".equals( property.getType().getName() )
					|| "org.hibernate.bytecode.internal.javassist.FieldHandler".equals( property.getType().getName() );
		}
}
