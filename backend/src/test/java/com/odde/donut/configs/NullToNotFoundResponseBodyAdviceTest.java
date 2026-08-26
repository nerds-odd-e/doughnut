package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.RecallPrompt;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

  @ParameterizedTest
  @MethodSource("supportCases")
  void supportsNonResponseEntityReturnTypesOnly(String methodName, boolean expected)
      throws NoSuchMethodException {
    Method method = SampleController.class.getDeclaredMethod(methodName);
    assertThat(
        advice.supports(new MethodParameter(method, -1), MappingJackson2HttpMessageConverter.class),
        is(expected));
  }

  static Stream<Arguments> supportCases() {
    return Stream.of(
        Arguments.of("returnsRecallPrompt", true), Arguments.of("returnsResponseEntity", false));
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

    assertThat(ex.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    assertThat(ex.getReason(), equalTo("Resource not found"));
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
