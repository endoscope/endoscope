package com.github.endoscope.basic.ds;

import com.github.endoscope.storage.jdbc.DataSourceProvider;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.sql.DataSource;

import static org.slf4j.LoggerFactory.getLogger;

public class BasicDataSourceProvider implements DataSourceProvider {
    private static final Logger log = getLogger(BasicDataSourceProvider.class);

    public DataSource create(String initParam){
        if( StringUtils.isNotBlank(initParam) && initParam.startsWith("jdbc:postgresql") ){
            log.info("Detected jdbc:postgresql storage param for Endoscope - initializing BasicDataSource");
            try{
                BasicDataSource dataSource = new BasicDataSource();
                dataSource.setDriverClassName("org.postgresql.Driver");
                //DEBUG: loglevel=2
                //INFO: loglevel=1
                //dataSource.setUrl(initParam + "&loglevel=1");

                dataSource.setUrl(initParam);

                return dataSource;
            }catch(Exception e){
                log.info("Failed to create custom Endoscope DataSource", e);
            }
        }
        return null;
    }
}
