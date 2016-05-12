package com.zorm.id;

import java.io.Serializable;

import com.zorm.session.SessionImplementor;
import com.zorm.sql.IdentifierGeneratingInsert;

public interface InsertGeneratedIdentifierDelegate {
	public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert();
	public Serializable performInsert(String insertSQL, SessionImplementor session, Binder binder);
}
