package com.zorm.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.util.ArrayHelper;

public class TypeInfoExtracter {
	
	private static final Log log = LogFactory.getLog(TypeInfoExtracter.class);
	
	private TypeInfoExtracter() {
	}

	public static LinkedHashSet<TypeInfo> extractTypeInfo(
			DatabaseMetaData metaData) {
		LinkedHashSet<TypeInfo> typeInfoSet = new LinkedHashSet<TypeInfo>();
		try{
			ResultSet resultSet = metaData.getTypeInfo();
			try{
				while(resultSet.next()){
					typeInfoSet.add(
							  new TypeInfo(
									  resultSet.getString("TYPE_NAME"), 
									  resultSet.getInt("DATA_TYPE"), 
									  interpretCreateParams( resultSet.getString( "CREATE_PARAMS" ) ), 
									  resultSet.getBoolean( "UNSIGNED_ATTRIBUTE" ),
										resultSet.getInt( "PRECISION" ),
										resultSet.getShort( "MINIMUM_SCALE" ),
										resultSet.getShort( "MAXIMUM_SCALE" ),
										resultSet.getBoolean( "FIXED_PREC_SCALE" ),
										resultSet.getString( "LITERAL_PREFIX" ),
										resultSet.getString( "LITERAL_SUFFIX" ),
										resultSet.getBoolean( "CASE_SENSITIVE" ),
										TypeSearchability.interpret( resultSet.getShort( "SEARCHABLE" ) ),
										TypeNullability.interpret( resultSet.getShort( "NULLABLE" ) ))
							);
				}
			}
			catch(SQLException e){
				log.warn("Error accessing type info result set :"+e.toString());
			}
			finally{
				try{
					resultSet.close();
				}
				catch(SQLException e){
					log.warn("Unable to release type info result set");
				}
			}
		}
		catch(SQLException e){
			log.warn("Unable to retrieve type info result set : "+e.toString());
		}
		return typeInfoSet;
	}

	private static String[] interpretCreateParams(String value) {
        if(value==null||value.length()==0){
        	return ArrayHelper.EMPTY_STRING_ARRAY;
        }
		return value.split(",");
	}
}
