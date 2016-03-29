package com.github.endoscope.storage.jdbc;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class DataSourceHelper {
    public static DataSource findDatasource(String dsJndiPath) {
        try{
            Context initContext = new InitialContext();
            DataSource ds = (DataSource)initContext.lookup(dsJndiPath);
            return ds;
        }catch(NamingException e){
            throw new RuntimeException(e);
        }
    }
}
