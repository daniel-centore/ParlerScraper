package com.danielcentore.scraper.parler.db;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import org.sqlite.JDBC;

public class SqliteDriverDecorator implements Driver {

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            final Driver driver = DriverManager.getDriver(JDBC.PREFIX);
            DriverManager.deregisterDriver(driver);
            DriverManager.registerDriver(new SqliteDriverDecorator(driver));
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Driver originalDriver;

    public SqliteDriverDecorator(Driver originalDriver) {
        this.originalDriver = originalDriver;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        final Connection connection = originalDriver.connect(url, info);

        connection.prepareStatement("PRAGMA foreign_keys = ON;").execute();

        return connection;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return originalDriver.acceptsURL(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return originalDriver.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return originalDriver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return originalDriver.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return originalDriver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return originalDriver.getParentLogger();
    }

}
