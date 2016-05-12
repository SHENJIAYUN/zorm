package com.zorm.id;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface Binder {
	public void bindValues(PreparedStatement ps) throws SQLException;
	public Object getEntity();
}
