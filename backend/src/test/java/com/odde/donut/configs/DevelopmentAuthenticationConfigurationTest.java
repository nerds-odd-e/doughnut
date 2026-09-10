package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties =
        "spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://127.0.0.1:3309/doughnut_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true}")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevelopmentAuthenticationConfigurationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ApplicationContext applicationContext;

  @Test
  void manualUserCanAuthenticateWithBasicAuth() throws Exception {
    mockMvc
        .perform(
            get("/login/continue")
                .param("from", "/notebooks")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic "
                        + HttpHeaders.encodeBasicAuth(
                            "manual", "password", StandardCharsets.UTF_8)))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/notebooks"));
  }

  @Test
  void testabilityControllersAreAbsent() {
    assertThat(applicationContext.containsBean("testabilityRestController"), is(false));
    assertThat(applicationContext.containsBean("notebookGitTestabilityController"), is(false));
  }
}
