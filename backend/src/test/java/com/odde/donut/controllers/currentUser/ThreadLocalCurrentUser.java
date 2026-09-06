package com.odde.donut.controllers.currentUser;

import com.odde.donut.entities.User;

/**
 * Test-only {@link CurrentUser} that stores the current user in a {@link ThreadLocal} so concurrent
 * test workers each get an independent slot even when they represent the same owner. Transparent
 * for single-threaded tests: {@code @BeforeEach} sets the user on the main thread and the test
 * reads it on the same thread. Production {@link CurrentUser} is unchanged.
 */
public class ThreadLocalCurrentUser extends CurrentUser {

  private final ThreadLocal<User> userByThread = new ThreadLocal<>();

  @Override
  public User getUser() {
    return userByThread.get();
  }

  @Override
  public void setUser(User user) {
    userByThread.set(user);
  }
}
