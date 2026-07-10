package com.odde.doughnut.configs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.RecallPrompt;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@SuppressWarnings("removal")
class NullToNotFoundResponseBodyAdviceTest {

  private final NullToNotFoundResponseBodyAdvice advice = new NullToNotFoundResponseBodyAdvice();

  @Test
  void supportsAppliesOnlyToNonResponseEntityReturnTypes() throws NoSuchMethodException {
    Method recallPromptMethod = SampleController.class.getDeclaredMethod("returnsRecallPrompt");
    Method responseEntityMethod = SampleController.class.getDeclaredMethod("returnsResponseEntity");
    assertThat(
            advice.supports(
                new MethodParameter(recallPromptMethod, -1),
                MappingJackson2HttpMessageConverter.class))
        .isTrue();
    assertThat(
            advice.supports(
                new MethodParameter(responseEntityMethod, -1),
                MappingJackson2HttpMessageConverter.class))
        .isFalse();
  }

  @Test
  void convertsNullBodyToNotFound() throws NoSuchMethodException {
    Method method = SampleController.class.getDeclaredMethod("returnsRecallPrompt");
    MethodParameter returnType = new MethodParameter(method, -1);
    ServerHttpRequest request =
        new ServletServerHttpRequest(new MockHttpServletRequest("GET", "/api/example"));
    ServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                advice.beforeBodyWrite(
                    null,
                    returnType,
                    MediaType.APPLICATION_JSON,
                    MappingJackson2HttpMessageConverter.class,
                    request,
                    response));

    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ex.getReason()).isEqualTo("Resource not found");
  }

  static class SampleController {
    @GetMapping
    RecallPrompt returnsRecallPrompt() {
      return null;
    }

    @GetMapping
    org.springframework.http.ResponseEntity<String> returnsResponseEntity() {
      return org.springframework.http.ResponseEntity.ok("ok");
    }
  }
}
