package com.odde.donut.services.notebookGit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Records every SQL string passed to {@link Connection#prepareStatement(String)} on a wrapped
 * connection, so a test can assert how many statements of a given shape a code path issued - for
 * example, proving a batched query replaced one round trip per attempted object.
 */
final class SqlStatementCallLog {

  private final List<String> preparedStatementSql = new ArrayList<>();

  long countMatching(String... requiredSubstrings) {
    return preparedStatementSql.stream()
        .filter(sql -> containsAll(sql, requiredSubstrings))
        .count();
  }

  private static boolean containsAll(String sql, String[] requiredSubstrings) {
    for (String required : requiredSubstrings) {
      if (!sql.contains(required)) {
        return false;
      }
    }
    return true;
  }

  Connection wrap(Connection target) {
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
              }
              try {
                return method.invoke(target, args);
              } catch (InvocationTargetException e) {
                throw e.getCause();
              }
            });
  }
}
