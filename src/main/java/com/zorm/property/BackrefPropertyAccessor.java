package com.zorm.property;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;

public class BackrefPropertyAccessor implements PropertyAccessor {

	private final String propertyName;
	private final String entityName;

	// cache these since they are stateless
	private final BackrefSetter setter; // this one could even be static...
	private final BackrefGetter getter;

	/**
	 * A placeholder for a property value, indicating that
	 * we don't know the value of the back reference
	 */
	public static final Serializable UNKNOWN = new Serializable() {
		public String toString() {
			return "<unknown>";
		}

		public Object readResolve() {
			return UNKNOWN;
		}
	};

	/**
	 * Constructs a new instance of BackrefPropertyAccessor.
	 *
	 * @param collectionRole The collection role which this back ref references.
	 * @param entityName The owner's entity name.
	 */
	public BackrefPropertyAccessor(String collectionRole, String entityName) {
		this.propertyName = collectionRole.substring( entityName.length() + 1 );
		this.entityName = entityName;

		this.setter = new BackrefSetter();
		this.getter = new BackrefGetter();
	}

	/**
	 * {@inheritDoc}
	 */
	public Setter getSetter(Class theClass, String propertyName) {
		return setter;
	}

	/**
	 * {@inheritDoc}
	 */
	public Getter getGetter(Class theClass, String propertyName) {
		return getter;
	}


	/**
	 * Internal implementation of a property setter specific to these back-ref properties.
	 */
	public static final class BackrefSetter implements Setter {

		/**
		 * {@inheritDoc}
		 */
		public Method getMethod() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public String getMethodName() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public void set(Object target, Object value, SessionFactoryImplementor factory) {
			// this page intentionally left blank :)
		}

	}


	/**
	 * Internal implementation of a property getter specific to these back-ref properties.
	 */
	public class BackrefGetter implements Getter {

		/**
		 * {@inheritDoc}
		 */
		public Object getForInsert(Object target, Map mergeMap, SessionImplementor session) {
			if ( session == null ) {
				return UNKNOWN;
			}
			else {
				return session.getPersistenceContext().getOwnerId( entityName, propertyName, target, mergeMap );
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public Member getMember() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public Object get(Object target) {
			return UNKNOWN;
		}

		/**
		 * {@inheritDoc}
		 */
		public Method getMethod() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public String getMethodName() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public Class getReturnType() {
			return Object.class;
		}
	}
}


