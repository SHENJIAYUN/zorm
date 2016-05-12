package com.zorm.mapping;

public interface MetaAttributable {
	public java.util.Map getMetaAttributes();
	public void setMetaAttributes(java.util.Map metas);
	public MetaAttribute getMetaAttribute(String name);
}
