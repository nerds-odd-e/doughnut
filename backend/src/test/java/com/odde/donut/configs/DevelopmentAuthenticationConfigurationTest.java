package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(
    properties =
        "spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://127.0.0.1:3309/doughnut_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true}")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevelopmentAuthenticationConfigurationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ApplicationContext applicationContext;
  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void deleteStoredSessions() {
    jdbcTemplate.update("DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME = 'manual'");
  }

  @Test
  void manualUserCanAuthenticateWithBasicAuth() throws Exception {
    signIn().andExpect(status().isFound()).andExpect(redirectedUrl("/notebooks"));
  }

  @Test
  void signedInSessionIsStoredInTheDatabaseAndFoundByItsCookie() throws Exception {
    Cookie sessionCookie = signInAndGetSessionCookie();

    currentUserInfo(sessionCookie).andExpect(jsonPath("$.externalIdentifier").value("manual"));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM SPRING_SESSION WHERE PRINCIPAL_NAME = 'manual'", Integer.class),
        is(1));
  }

  @Test
  void logoutSignsOutTheSessionCookie() throws Exception {
    Cookie sessionCookie = signInAndGetSessionCookie();

    mockMvc.perform(post("/logout").cookie(sessionCookie));

    currentUserInfo(sessionCookie).andExpect(jsonPath("$.externalIdentifier").value(nullValue()));
  }

  @Test
  void unknownSessionCookieIsSignedOut() throws Exception {
    currentUserInfo(new Cookie("SESSION", "dW5rbm93bg=="))
        .andExpect(jsonPath("$.externalIdentifier").value(nullValue()));
  }

  private ResultActions signIn() throws Exception {
    return mockMvc.perform(
        get("/login/continue")
            .param("from", "/notebooks")
            .header(
                HttpHeaders.AUTHORIZATION,
                "Basic "
                    + HttpHeaders.encodeBasicAuth("manual", "password", StandardCharsets.UTF_8)));
  }

  private Cookie signInAndGetSessionCookie() throws Exception {
    return signIn().andReturn().getResponse().getCookie("SESSION");
  }

  private ResultActions currentUserInfo(Cookie sessionCookie) throws Exception {
    return mockMvc.perform(get("/api/user/current-user-info").cookie(sessionCookie));
  }

  @Test
  void testabilityControllersAreAbsent() {
    assertThat(applicationContext.containsBean("testabilityRestController"), is(false));
    assertThat(applicationContext.containsBean("notebookGitTestabilityController"), is(false));
  }
}
