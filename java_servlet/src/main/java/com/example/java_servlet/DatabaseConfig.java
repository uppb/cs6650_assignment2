package com.example.java_servlet;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.commons.dbcp2.BasicDataSource;


public class DatabaseConfig {
  public static BasicDataSource getDataSource() {
    BasicDataSource dataSource = new BasicDataSource();
    //dataSource.setDriverClassName("org.postgresql.Driver");
    //dataSource.setUrl("jdbc:postgresql://cs6650.c6xff3divtzz.us-west-2.rds.amazonaws.com:5432/cs6650");
    //dataSource.setUrl("jdbc:postgresql://localhost:5432/cs6650");
    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
    dataSource.setUrl("jdbc:mysql://cs6650-mysql.c6xff3divtzz.us-west-2.rds.amazonaws.com:3306/cs6650");
    //dataSource.setUrl("jdbc:mysql://localhost:3306/cs6650");
    dataSource.setUsername("some username");
    dataSource.setPassword("some password");

    dataSource.setInitialSize(20); // initial number of connections
    dataSource.setMaxTotal(45);   // max number of connections
    dataSource.setMaxIdle(30);     // max number of idle connections
    dataSource.setMinIdle(15);     // min number of idle connections
    dataSource.setMaxWaitMillis(30000);  // timeout 30 seconds
    return dataSource;
  }
}





