package com.odde.donut.services.notebookGit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Records JDBC prepare/execute traffic on a wrapped connection so a test can observe how many
 * statements of a given shape a code path issued — including parameters and simple results — not
 * only the SQL text passed to {@link Connection#prepareStatement(String)}.
 *
 * <p>JDBC executions are not wire round trips; a single {@code executeQuery}/{@code executeUpdate}
 * is one driver call and may still be satisfied from a pool or server-side prepared handle.
 *
 * <p>For Spring controller tests, import {@link SqlStatementCallLogDataSourceConfig} on the shared
 * controller test base so the primary {@link DataSource} wraps connections only while a log is
 * {@link #activate() activated}. Scope activation to the real controller call (and its transaction
 * completion), not fixture setup or assertion reads.
 */
public final class SqlStatementCallLog {

  private static final ThreadLocal<SqlStatementCallLog> ACTIVE = new ThreadLocal<>();

  private final List<String> preparedStatementSql = new ArrayList<>();
  private final List<Execution> executions = new ArrayList<>();

  public record Execution(
      String sql,
      String method,
      List<Object> parameters,
      int result,
      List<Integer> objectTypes,
      List<Integer> fetchedObjectByteLengths) {}

  public AutoCloseable activate() {
    ACTIVE.set(this);
    return () -> {
      if (ACTIVE.get() == this) {
        ACTIVE.remove();
      }
    };
  }

  public List<Execution> executions() {
    return List.copyOf(executions);
  }

  public long countMatching(String... requiredSubstrings) {
    return preparedStatementSql.stream()
        .filter(sql -> containsAll(sql, requiredSubstrings))
        .count();
  }

  public long countExecutionsMatching(String... requiredSubstrings) {
    return executions.stream().filter(e -> containsAll(e.sql(), requiredSubstrings)).count();
  }

  public long countObjectFetches() {
    return executions.stream().filter(SqlStatementCallLog::isObjectFetch).count();
  }

  public long countObjectFetchesReturningType(int objectType) {
    return executions.stream()
        .filter(SqlStatementCallLog::isObjectFetch)
        .filter(e -> e.objectTypes().contains(objectType))
        .count();
  }

  /** Sum of {@code object_bytes} lengths returned by accepted-object fetches. */
  public long fetchedObjectBytes() {
    return executions.stream()
        .filter(SqlStatementCallLog::isObjectFetch)
        .mapToLong(e -> e.fetchedObjectByteLengths().stream().mapToInt(Integer::intValue).sum())
        .sum();
  }

  /**
   * Objects named in existence-check {@code IN (...)} clauses (attempted writes before dedupe), not
   * newly inserted rows.
   */
  public int attemptedObjectIds() {
    return executions.stream()
        .filter(e -> containsAll(e.sql(), "notebook_git_accepted_object", " IN ("))
        .mapToInt(e -> Math.max(0, e.parameters().size() - 1))
        .sum();
  }

  /** Sum of JDBC update counts from {@code INSERT INTO notebook_git_accepted_object}. */
  public int objectInsertRows() {
    return executions.stream()
        .filter(e -> e.sql().contains("INSERT INTO notebook_git_accepted_object"))
        .mapToInt(Execution::result)
        .sum();
  }

  private static boolean isObjectFetch(Execution execution) {
    return "executeQuery".equals(execution.method())
        && containsAll(execution.sql(), "notebook_git_accepted_object", "object_bytes");
  }

  private static boolean containsAll(String sql, String... requiredSubstrings) {
    for (String required : requiredSubstrings) {
      if (!sql.contains(required)) {
        return false;
      }
    }
    return true;
  }

  public Connection wrap(Connection target) {
    return (Connection)
        Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
              if ("prepareStatement".equals(method.getName())
                  && args != null
                  && args.length > 0
                  && args[0] instanceof String sql) {
                preparedStatementSql.add(sql);
                PreparedStatement statement = (PreparedStatement) invoke(target, method, args);
                return wrapPreparedStatement(sql, statement);
              }
              return invoke(target, method, args);
            });
  }

  private PreparedStatement wrapPreparedStatement(String sql, PreparedStatement target) {
    List<Object> parameters = new ArrayList<>();
    return (PreparedStatement)
        Proxy.newProxyInstance(
            PreparedStatement.class.getClassLoader(),
            new Class<?>[] {PreparedStatement.class},
            (proxy, method, args) -> {
              String name = method.getName();
              if (name.startsWith("set") && args != null && args.length >= 2) {
                int index = (Integer) args[0];
                ensureSize(parameters, index);
                parameters.set(index - 1, args[1]);
              }
              if ("clearParameters".equals(name)) {
                parameters.clear();
              }
              if ("executeQuery".equals(name)) {
                ResultSet resultSet = (ResultSet) invoke(target, method, args);
                List<Integer> objectTypes = new ArrayList<>();
                List<Integer> fetchedObjectByteLengths = new ArrayList<>();
                executions.add(
                    new Execution(
                        sql,
                        name,
                        List.copyOf(parameters),
                        0,
                        objectTypes,
                        fetchedObjectByteLengths));
                return wrapResultSet(sql, resultSet, objectTypes, fetchedObjectByteLengths);
              }
              if ("executeUpdate".equals(name) || "execute".equals(name)) {
                Object result = invoke(target, method, args);
                int count =
                    result instanceof Boolean
                        ? target.getUpdateCount()
                        : ((Integer) result).intValue();
                executions.add(
                    new Execution(sql, name, List.copyOf(parameters), count, List.of(), List.of()));
                return result;
              }
              return invoke(target, method, args);
            });
  }

  private static ResultSet wrapResultSet(
      String sql,
      ResultSet target,
      List<Integer> objectTypes,
      List<Integer> fetchedObjectByteLengths) {
    boolean captureObjectType =
        sql.contains("object_type") && sql.contains("notebook_git_accepted_object");
    return (ResultSet)
        Proxy.newProxyInstance(
            ResultSet.class.getClassLoader(),
            new Class<?>[] {ResultSet.class},
            (proxy, method, args) -> {
              Object result = invoke(target, method, args);
              if (captureObjectType
                  && "next".equals(method.getName())
                  && Boolean.TRUE.equals(result)) {
                objectTypes.add(target.getInt(1));
                byte[] bytes = target.getBytes(2);
                fetchedObjectByteLengths.add(bytes == null ? 0 : bytes.length);
              }
              return result;
            });
  }

  private static void ensureSize(List<Object> parameters, int index) {
    while (parameters.size() < index) {
      parameters.add(null);
    }
  }

  private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
    try {
      return method.invoke(target, args);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  static DataSource recordingDataSource(DataSource target) {
    return new RecordingDataSource(target);
  }

  static final class RecordingDataSource extends DelegatingDataSource {
    RecordingDataSource(DataSource target) {
      super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
      return maybeWrap(getTargetDataSource().getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
      return maybeWrap(super.getConnection(username, password));
    }

    private static Connection maybeWrap(Connection connection) {
      SqlStatementCallLog active = ACTIVE.get();
      return active == null ? connection : active.wrap(connection);
    }
  }
}
