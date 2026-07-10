package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class ApplicationControllerProdDeepLinkTests {

  private static final String SPA_PUBLIC_BASE = "http://127.0.0.1:18888";

  private ApplicationController controller;
  private Environment env;
  private RestTemplate restTemplate;
  private MockRestServiceServer mockUpstream;

  @BeforeEach
  void setUp() {
    env = mock(Environment.class);
    when(env.acceptsProfiles(any(Profiles.class))).thenReturn(true);
    restTemplate = new RestTemplate();
    mockUpstream = MockRestServiceServer.bindTo(restTemplate).build();
    controller = new ApplicationController(env, restTemplate, SPA_PUBLIC_BASE);
  }

  @AfterEach
  void tearDown() {
    mockUpstream.verify();
  }

  @Test
  void prodDeepLinkRoutesReturnHtmlFromConfiguredSpaOrigin() {
    String shell = "<!DOCTYPE html><html><body>spa-shell</body></html>";
    mockUpstream
        .expect(requestTo("http://127.0.0.1:18888/"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(shell, MediaType.TEXT_HTML));

    Object result = controller.spaDeepLink();

    assertThat(result, instanceOf(ResponseEntity.class));
    @SuppressWarnings("unchecked")
    ResponseEntity<byte[]> response = (ResponseEntity<byte[]>) result;
    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(new String(response.getBody(), StandardCharsets.UTF_8), containsString("spa-shell"));
    assertThat(response.getHeaders().getContentType(), equalTo(MediaType.TEXT_HTML));
  }

  @Test
  void prodDeepLinkPropagatesUpstreamFailure() {
    mockUpstream
        .expect(requestTo("http://127.0.0.1:18888/"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    assertThrows(RestClientException.class, () -> controller.spaDeepLink());
  }
}
