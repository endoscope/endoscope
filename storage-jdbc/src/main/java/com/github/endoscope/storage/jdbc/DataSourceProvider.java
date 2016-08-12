package com.github.endoscope.storage.jdbc;

import javax.sql.DataSource;

/**
 * Implement com.github.endoscope.storage.jdbc.CustomDataSourceFactory in order to provide your own
 * implementation of DataSource. It has priority over JNDI DS.
 */
public interface DataSourceProvider {
    String IMPLEMENTATION_CLASS_NAME = "com.github.endoscope.storage.jdbc.CustomDataSourceProvider";
    String BASIC_DS_PROVIDER = "com.github.endoscope.basic.ds.BasicDataSourceProvider";
    DataSource create();
}
