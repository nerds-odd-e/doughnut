package com.odde.doughnut.controllers;

import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;

abstract class RecallLogBackfillControllerTestBase extends MemoryTrackerControllerTestBase {
  @Autowired DataSource dataSource;

  protected abstract String migrationSql();

  void applyBackfill() {
    makeMe.entityPersister.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      ScriptUtils.executeSqlScript(connection, new ClassPathResource(migrationSql()));
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }
}
