package com.zorm.jdbc;

import com.zorm.dialect.Dialect;
import com.zorm.service.Service;
import com.zorm.session.SessionImplementor;
import com.zorm.type.descriptor.LobCreator;

public interface JdbcServices extends Service{

	//获取数据库连接元数据
	ExtractedDatabaseMetaData getExtractedMetaDataSupport();

	//获取数据库的dialect
	public Dialect getDialect();

	public SqlExceptionHelper getSqlExceptionHelper();

	public LobCreator getLobCreator(LobCreationContext lobCreationContext);


}
