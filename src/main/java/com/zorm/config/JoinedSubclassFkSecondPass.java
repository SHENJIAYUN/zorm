package com.zorm.config;

import java.util.Map;

import com.zorm.config.annotations.TableBinder;
import com.zorm.exception.MappingException;
import com.zorm.mapping.JoinedSubclass;
import com.zorm.mapping.SimpleValue;

public class JoinedSubclassFkSecondPass extends FkSecondPass {
	private JoinedSubclass entity;
	private Mappings mappings;

	public JoinedSubclassFkSecondPass(
			JoinedSubclass entity,
			Ejb3JoinColumn[] inheritanceJoinedColumns,
			SimpleValue key,
			Mappings mappings) {
		super( key, inheritanceJoinedColumns );
		this.entity = entity;
		this.mappings = mappings;
	}

	public String getReferencedEntityName() {
		return entity.getSuperclass().getEntityName();
	}

	public boolean isInPrimaryKey() {
		return true;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		TableBinder.bindFk( entity.getSuperclass(), entity, columns, value, false, mappings );
	}
}
