package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.donut.testability.SpringTestBase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;

class DatabaseTimeZoneTest extends SpringTestBase {
  @Autowired DataSource dataSource;

  @Test
  void sessionTimeZoneIsPinnedToUtc() throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery("SELECT @@session.time_zone");
      resultSet.next();
      assertThat(resultSet.getString(1), is("+00:00"));
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Test
  void instantRoundTripsExactlyThroughJdbc() throws Exception {
    long knownEpochMillis = 1750000000000L;
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery("SELECT FROM_UNIXTIME(1750000000)");
      resultSet.next();
      Timestamp roundTripped = resultSet.getTimestamp(1);
      assertThat(roundTripped.getTime(), is(knownEpochMillis));
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }
}
