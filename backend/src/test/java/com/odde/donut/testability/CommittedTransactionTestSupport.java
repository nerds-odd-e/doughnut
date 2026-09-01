package com.odde.donut.testability;

import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class CommittedTransactionTestSupport {
  private CommittedTransactionTestSupport() {}

  public static <T> T inCommittedTransaction(
      PlatformTransactionManager transactionManager, Supplier<T> action) {
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return template.execute(status -> action.get());
  }

  public static void inCommittedTransaction(
      PlatformTransactionManager transactionManager, Runnable action) {
    inCommittedTransaction(
        transactionManager,
        () -> {
          action.run();
          return null;
        });
  }
}
