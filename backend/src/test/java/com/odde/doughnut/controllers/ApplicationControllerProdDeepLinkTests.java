package com.odde.doughnut.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class ApplicationControllerProdDeepLinkTests {

  private static final String SPA_PUBLIC_BASE = "http://127.0.0.1:18888";

  private MockMvc mockMvc;
  private Environment env;
  private RestTemplate restTemplate;
  private MockRestServiceServer mockUpstream;

  @BeforeEach
  void setUp() {
    env = mock(Environment.class);
    when(env.acceptsProfiles(any(Profiles.class))).thenReturn(true);
    restTemplate = new RestTemplate();
    mockUpstream = MockRestServiceServer.bindTo(restTemplate).build();
    ApplicationController controller =
        new ApplicationController(env, restTemplate, SPA_PUBLIC_BASE);
    mockMvc =
        standaloneSetup(controller)
            .setControllerAdvice(new RestClientExceptionAsInternalServerError())
            .build();
  }

  @AfterEach
  void tearDown() {
    mockUpstream.verify();
  }

  @Test
  void prodDeepLinkUnderDReturnsHtmlFromConfiguredSpaOrigin() throws Exception {
    String shell = "<!DOCTYPE html><html><head></head><body>spa-shell</body></html>";
    mockUpstream
        .expect(requestTo("http://127.0.0.1:18888/"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(shell, MediaType.TEXT_HTML));

    mockMvc
        .perform(get("/d/notebooks/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(containsString("spa-shell")));
  }

  @Test
  void prodDeepLinkUnderNReturnsHtmlFromConfiguredSpaOrigin() throws Exception {
    String shell = "<html lang=\"en\"><title>t</title></html>";
    mockUpstream
        .expect(requestTo("http://127.0.0.1:18888/"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(shell, MediaType.TEXT_HTML));

    mockMvc
        .perform(get("/n42"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(containsString("<html lang=\"en\">")));
  }

  @Test
  void prodDeepLinkPropagatesUpstreamFailure() throws Exception {
    mockUpstream
        .expect(requestTo("http://127.0.0.1:18888/"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    mockMvc.perform(get("/d/x")).andExpect(status().isInternalServerError());
  }

  @ControllerAdvice
  static final class RestClientExceptionAsInternalServerError {
    @ExceptionHandler(RestClientException.class)
    ResponseEntity<Void> map(RestClientException ignored) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
