package com.odde.doughnut.controllers.currentUser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestAccessTokenResolver;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CurrentUserFetcherFromRequestTest {

  @Autowired UserRepository userRepository;
  @Autowired UserService userService;
  @Autowired TestAccessTokenResolver testAccessTokenResolver;
  @Autowired MakeMe makeMe;

  CurrentUserFetcherFromRequest fetcherWith(MockHttpServletRequest request, boolean withResolver) {
    Optional<TestAccessTokenResolver> resolver =
        withResolver ? Optional.of(testAccessTokenResolver) : Optional.empty();
    return new CurrentUserFetcherFromRequest(request, userRepository, userService, resolver);
  }

  @Test
  void resolvesUserByTestAccessTokenAndExternalIdentifier() {
    User user = makeMe.aUser().please();
    String extId = user.getExternalIdentifier();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer access-token-of-" + extId);
    CurrentUserFetcherFromRequest fetcher = fetcherWith(request, true);
    assertThat(fetcher.getUser().getId(), equalTo(user.getId()));
    assertThat(fetcher.getExternalIdentifier(), equalTo(extId));
  }

  @Test
  void unknownIdentifierYieldsNoUser() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer access-token-of-nobody-here");
    assertNull(fetcherWith(request, true).getUser());
  }

  @Test
  void testTokenTreatedAsRegularTokenWhenResolverAbsent() {
    User user = makeMe.aUser().please();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer access-token-of-" + user.getExternalIdentifier());
    assertThrows(ResponseStatusException.class, () -> fetcherWith(request, false));
  }
}
