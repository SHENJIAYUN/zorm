package com.zorm.tuple;

import com.zorm.engine.IdentifierValue;
import com.zorm.id.IdentifierGenerator;
import com.zorm.id.PostInsertIdentifierGenerator;
import com.zorm.type.Type;

public class IdentifierProperty extends Property{
	private boolean virtual;
	private boolean embedded;
	private IdentifierValue unsavedValue;
	private IdentifierGenerator identifierGenerator;
	private boolean identifierAssignedByInsert;
	private boolean hasIdentifierMapper;
	
	public IdentifierProperty(
			String name,
			String node,
			Type type,
			boolean embedded,
			IdentifierValue unsavedValue,
			IdentifierGenerator identifierGenerator) {
		super(name, node, type);
		this.virtual = false;
		this.embedded = embedded;
		this.hasIdentifierMapper = false;
		this.unsavedValue = unsavedValue;
		this.identifierGenerator = identifierGenerator;
		this.identifierAssignedByInsert = identifierGenerator instanceof PostInsertIdentifierGenerator;
	}
	
	public IdentifierProperty(
	        Type type,
	        boolean embedded,
			boolean hasIdentifierMapper,
			IdentifierValue unsavedValue,
	        IdentifierGenerator identifierGenerator) {
		super(null, null, type);
		this.virtual = true;
		this.embedded = embedded;
		this.hasIdentifierMapper = hasIdentifierMapper;
		this.unsavedValue = unsavedValue;
		this.identifierGenerator = identifierGenerator;
		this.identifierAssignedByInsert = identifierGenerator instanceof PostInsertIdentifierGenerator;
	}

	public IdentifierGenerator getIdentifierGenerator() {
		return identifierGenerator;
	}

	public boolean isVirtual() {
		return virtual;
	}

	public boolean isEmbedded() {
		return embedded;
	}

	public IdentifierValue getUnsavedValue() {
		return unsavedValue;
	}

	public boolean hasIdentifierMapper() {
		return hasIdentifierMapper;
	}

	public boolean isIdentifierAssignedByInsert() {
		return identifierAssignedByInsert;
	}

}
