package com.odde.donut.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationControllerTest {
  @Autowired private MockMvc mockMvc;

  @Test
  void loginContinueRedirectsToFromParam() throws Exception {
    mockMvc
        .perform(
            get("/login/continue")
                .param("from", "/notebooks")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic "
                        + HttpHeaders.encodeBasicAuth("user", "password", StandardCharsets.UTF_8)))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/notebooks"));
  }

  @Test
  void usersIdentifyIsNotMapped() throws Exception {
    mockMvc.perform(get("/users/identify")).andExpect(status().isNotFound());
  }
}
