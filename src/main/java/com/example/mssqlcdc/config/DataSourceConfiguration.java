package com.example.mssqlcdc.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author gunha
 * @version 0.1
 * @since 2023-02-28 오후 5:24
 */
@Configuration
public class DataSourceConfiguration {

    @Bean
    public DataSource getDataSource() {
        /**
         * 현재 하드코딩된 상태
         */
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.url("jdbc:sqlserver://localhost:1433;databaseName=SQL2019CDC;encrypt=true;trustServerCertificate=true");
        dataSourceBuilder.username("sa");
        dataSourceBuilder.password("password1!");
        dataSourceBuilder.driverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        return dataSourceBuilder.build();
    }
}
