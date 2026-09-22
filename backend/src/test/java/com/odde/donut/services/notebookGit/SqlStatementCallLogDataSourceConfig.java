package com.odde.donut.services.notebookGit;

import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Wraps the primary pooled {@link DataSource} once for the shared controller-test Spring context so
 * {@link SqlStatementCallLog#activate()} records the transaction connection used by the JDBC Git
 * store.
 */
@TestConfiguration
public class SqlStatementCallLogDataSourceConfig {
  @Bean
  static BeanPostProcessor sqlStatementCallLogDataSource() {
    return new BeanPostProcessor() {
      private final AtomicInteger wrapped = new AtomicInteger();

      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName)
          throws BeansException {
        if (bean instanceof DataSource dataSource
            && !(bean instanceof SqlStatementCallLog.RecordingDataSource)
            && !(bean instanceof DelegatingDataSource)
            && wrapped.compareAndSet(0, 1)) {
          return SqlStatementCallLog.recordingDataSource(dataSource);
        }
        return bean;
      }
    };
  }
}
