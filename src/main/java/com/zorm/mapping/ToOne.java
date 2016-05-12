package com.zorm.mapping;

import com.zorm.FetchMode;
import com.zorm.config.Mappings;
import com.zorm.exception.MappingException;
import com.zorm.type.Type;
import com.zorm.util.ReflectHelper;

public abstract class ToOne extends SimpleValue implements Fetchable{
	private FetchMode fetchMode;
	protected String referencedPropertyName;
	private String referencedEntityName;
	private boolean embedded;
	private boolean lazy = true;
	protected boolean unwrapProxy;

	protected ToOne(Mappings mappings, Table table) {
		super( mappings, table );
	}

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public void setFetchMode(FetchMode fetchMode) {
		this.fetchMode=fetchMode;
	}

	public abstract void createForeignKey() throws MappingException;
	public abstract Type getType() throws MappingException;
	
	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public void setReferencedPropertyName(String name) {
		referencedPropertyName = name==null ? null : name.intern();
	}
	
	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName==null ? 
				null : referencedEntityName.intern();
	}

	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		if (referencedEntityName==null) {
			referencedEntityName = ReflectHelper.reflectedPropertyClass( className, propertyName ).getName();
		}
	}

	public boolean isTypeSpecified() {
		return referencedEntityName!=null;
	}
	
	public boolean isLazy() {
		return lazy;
	}
	
	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}
	
	public boolean isUnwrapProxy() {
		return unwrapProxy;
	}

	public void setUnwrapProxy(boolean unwrapProxy) {
		this.unwrapProxy = unwrapProxy;
	}

}
