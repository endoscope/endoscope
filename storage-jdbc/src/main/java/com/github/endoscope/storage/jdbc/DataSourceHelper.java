package com.github.endoscope.storage.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import static org.slf4j.LoggerFactory.getLogger;

public class DataSourceHelper {
    private static final Logger log = getLogger(DataSourceHelper.class);

    public static DataSource findDatasource(String initParam) {
        //try dedicated plugin first
        DataSource ds = createCustomDataSource("com.github.endoscope.basic.ds.BasicDataSourceProvider", initParam);

        //try custom DS provider implementation - use init param as class name
        if( ds == null ){
            ds = createCustomDataSource(initParam, null);
        }

        //try JNDI reference
        if( ds == null ){
            ds = findJndiDatasource(initParam);
        }
        return ds;
    }

    private static DataSource createCustomDataSource(String className, String initParam) {
        try {
            Class c = Class.forName(className);
            DataSourceProvider dsp = (DataSourceProvider)c.newInstance();
            DataSource ds = dsp.create(initParam);
            if( ds != null ){
                log.debug("Created custom DataSource: {}", className);
            }
            return ds;
        } catch (Exception  e) {
            log.debug("Didn't create DataSource: {}", e.getMessage());
        }
        return null;
    }

    private static DataSource findJndiDatasource(String initParam) {
        if( StringUtils.isBlank(initParam) ){
            log.debug("Empty JNDI - skipping DataSource lookup");
            return null;
        }

        try{
            Context initContext = new InitialContext();
            DataSource ds = (DataSource)initContext.lookup(initParam);
            if( ds != null ){
                log.debug("Found DataSource in JNDI: " + initParam);
            }
            return ds;
        }catch(NamingException e){
            log.debug("Failed to lookup JNDI DataSource");
        }
        return null;
    }
}
