package com.zorm.mapping;

import com.zorm.config.Mappings;
import com.zorm.exception.MappingException;
import com.zorm.type.CollectionType;
import com.zorm.type.PrimitiveType;
import com.zorm.util.ReflectHelper;

public class Array extends List{
	private String elementClassName;

	public Array(Mappings mappings, PersistentClass owner) {
		super( mappings, owner );
	}

	public Class getElementClass() throws MappingException {
		if (elementClassName==null) {
			com.zorm.type.Type elementType = getElement().getType();
			return isPrimitiveArray() ?
				( (PrimitiveType) elementType ).getPrimitiveClass() :
				elementType.getReturnedClass();
		}
		else {
			try {
				return ReflectHelper.classForName(elementClassName);
			}
			catch (ClassNotFoundException cnfe) {
				throw new MappingException(cnfe);
			}
		}
	}

	@Override
    public CollectionType getDefaultCollectionType() throws MappingException {
		return getMappings().getTypeResolver()
				.getTypeFactory()
				.array( getRole(), getReferencedPropertyName(), getElementClass() );
	}

	@Override
    public boolean isArray() {
		return true;
	}

	/**
	 * @return Returns the elementClassName.
	 */
	public String getElementClassName() {
		return elementClassName;
	}
	/**
	 * @param elementClassName The elementClassName to set.
	 */
	public void setElementClassName(String elementClassName) {
		this.elementClassName = elementClassName;
	}
}
