package com.odde.doughnut.testability;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.UserRepository;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class TestAccessTokenResolver {
  private static final String PREFIX = "access-token-of-";

  private final UserRepository userRepository;

  public TestAccessTokenResolver(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public boolean handles(String token) {
    return token.startsWith(PREFIX) && token.length() > PREFIX.length();
  }

  public Optional<User> resolve(String token) {
    String identifier = token.substring(PREFIX.length());
    return Optional.ofNullable(userRepository.findByExternalIdentifier(identifier));
  }
}
