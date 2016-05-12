package com.zorm.mapping;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import com.zorm.engine.CascadeStyle;
import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.exception.PropertyNotFoundException;
import com.zorm.property.Getter;
import com.zorm.property.PropertyAccessor;
import com.zorm.property.PropertyAccessorFactory;
import com.zorm.property.Setter;
import com.zorm.type.Type;
import com.zorm.util.ArrayHelper;

public class Property implements Serializable,MetaAttributable{
  private String name;
  private Value value;
  private String cascade;
  private boolean updateable = true;
  private boolean insertable = true;
  private boolean selectable = true;
  private boolean optimisticLocked = true;
	private PropertyGeneration generation = PropertyGeneration.NEVER;
	private String propertyAccessorName;
	private boolean lazy;
	private boolean optional;
	private String nodeName;
	private java.util.Map metaAttributes;
	private PersistentClass persistentClass;
	private boolean naturalIdentifier;
	private boolean lob;
	
	@Override
	public Map getMetaAttributes() {
		return null;
	}
	@Override
	public void setMetaAttributes(Map metas) {
	}
	
	@Override
	public MetaAttribute getMetaAttribute(String name) {
		return null;
	}
	
	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}
	
	public Value getValue() {
		return value;
	}
	
	public String getName() {
		return name;
	}
	
	public void setPersistentClass(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}
	
	public Type getType() {
		return value.getType();
	}
	public void setName(String name) {
		this.name = name==null?null:name.intern();
	}
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	public void setValue(Value value) {
		this.value = value;
	}
	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}
	public void setCascade(String cascade) {
		this.cascade = cascade;
	}
	public void setPropertyAccessorName(String type) {
		propertyAccessorName = type;
	}
	public void setLob(boolean lob) {
		this.lob = lob;
	}
	
	  public void setUpdateable(boolean mutable) {
			this.updateable = mutable;
		}

		public void setInsertable(boolean insertable) {
			this.insertable = insertable;
		}
		
		public void setOptimisticLocked(boolean b) {
			this.optimisticLocked = b;
		}
		public boolean isBasicPropertyAccessor() {
			return propertyAccessorName==null||"property".equals(propertyAccessorName);
		}
		public boolean isLazy() {
			return lazy;
		}
		public int getColumnSpan() {
			return value.getColumnSpan();
		}
		public PersistentClass getPersistentClass() {
			return persistentClass;
		}
		
		public Iterator getColumnIterator() {
		  return value.getColumnIterator();
	    }
		public boolean isSelectable() {
			return selectable;
		}
		public boolean isLob() {
			return lob;
		}
		public boolean isOptional() {
			return optional || isNullable();
		}
		private boolean isNullable() {
			return value==null || value.isNullable();
		}
		public String getNodeName() {
			return nodeName;
		}
		public String getPropertyAccessorName() {
			return propertyAccessorName;
		}
		public boolean isNaturalIdentifier() {
			return naturalIdentifier;
		}
		
		public boolean isUpdateable() {
		// if the property mapping consists of all formulas,
		// make it non-updateable
		return updateable && !ArrayHelper.isAllFalse( value.getColumnUpdateability() );
	}
		
		public boolean isInsertable() {
			final boolean[] columnInsertability = value.getColumnInsertability();
			return insertable && (
					columnInsertability.length==0 ||
					!ArrayHelper.isAllFalse( columnInsertability )
				);
		}
		
		 public PropertyGeneration getGeneration() {
		        return generation;
		    }
		
		 public boolean isOptimisticLocked() {
				return optimisticLocked;
			}
		 
		public Getter getGetter(Class clazz) {
			return getPropertyAccessor(clazz).getGetter( clazz, name );
		}
		
		public PropertyAccessor getPropertyAccessor(Class clazz) throws MappingException {
			return PropertyAccessorFactory.getPropertyAccessor( clazz, getPropertyAccessorName() );
		}
		
		public Setter getSetter(Class clazz) throws PropertyNotFoundException, MappingException {
			return getPropertyAccessor(clazz).getSetter(clazz, name);
		}
		
		public boolean isValid(Mapping mapping) {
			return getValue().isValid(mapping);
		}
		
		public boolean isComposite() {
			return false;
		}
		
		private static CascadeStyle getCollectionCascadeStyle(Type elementType, String cascade) {
		   return getCascadeStyle( cascade );
		}
		
		private static CascadeStyle getCascadeStyle(String cascade) {
			if ( cascade==null || cascade.equals("none") ) {
				return CascadeStyle.NONE;
			}
			else {
				StringTokenizer tokens = new StringTokenizer(cascade, ", ");
				CascadeStyle[] styles = new CascadeStyle[ tokens.countTokens() ] ;
				int i=0;
				while ( tokens.hasMoreTokens() ) {
					styles[i++] = CascadeStyle.getCascadeStyle( tokens.nextToken() );
				}
				return new CascadeStyle.MultipleCascadeStyle(styles);
			}		
		}
		
		public CascadeStyle getCascadeStyle() throws MappingException {
			Type type = value.getType();
		    if ( type.isCollectionType() ) {
				return getCollectionCascadeStyle( ( (Collection) value ).getElement().getType(), cascade );
			}
			else {
				return getCascadeStyle( cascade );			
			}
		}
}
