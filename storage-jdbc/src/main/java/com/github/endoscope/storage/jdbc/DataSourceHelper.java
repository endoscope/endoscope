package com.github.endoscope.storage.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import java.lang.reflect.Constructor;

import static org.slf4j.LoggerFactory.getLogger;

public class DataSourceHelper {
    private static final Logger log = getLogger(DataSourceHelper.class);

    public static DataSource findDatasource(String initParam) {
        DataSource ds = createCustomDataSource();
        if( ds == null ){
            ds = createBasicDataSource(initParam);
        }
        if( ds == null ){
            ds = findJndiDatasource(initParam);
        }
        return ds;
    }

    private static DataSource createBasicDataSource(String initParam) {
        try {
            Class c = Class.forName(DataSourceProvider.BASIC_DS_PROVIDER);
            Constructor constructor = c.getConstructor(String.class);
            DataSourceProvider dsp = (DataSourceProvider) constructor.newInstance(initParam);
            DataSource ds = dsp.create();
            if( ds != null ){
                log.debug("Created Basic Data Source");
            }
            return ds;
        } catch (Exception  e) {
            log.debug("Didn't create Basic DataSource: {}", e.getMessage());
        }
        return null;
    }

    private static DataSource createCustomDataSource() {
        try {
            Class c = Class.forName(DataSourceProvider.IMPLEMENTATION_CLASS_NAME);
            DataSourceProvider dsp = (DataSourceProvider)c.newInstance();
            DataSource ds = dsp.create();
            if( ds != null ){
                log.debug("Created custom DataSource");
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
