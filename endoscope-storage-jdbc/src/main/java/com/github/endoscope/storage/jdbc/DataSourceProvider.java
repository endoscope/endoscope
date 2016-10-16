package com.github.endoscope.storage.jdbc;

import javax.sql.DataSource;

/**
 * Provide implementation class name in storage init parameter property in order
 * pass DataSource of your choice to JDBC storage.
 */
public interface DataSourceProvider {
    DataSource create(String initParam);
}
