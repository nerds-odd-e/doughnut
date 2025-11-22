package com.odde.doughnut.configs;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(annotations = RestController.class)
public class NullToNotFoundResponseBodyAdvice implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(
      @NonNull MethodParameter returnType,
      @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
    Class<?> returnTypeClass = returnType.getParameterType();
    // Only apply to methods that return non-void, non-ResponseEntity types
    // ResponseEntity means the controller is explicitly handling the response
    return !void.class.equals(returnTypeClass)
        && !Void.class.equals(returnTypeClass)
        && !ResponseEntity.class.isAssignableFrom(returnTypeClass);
  }

  @Override
  @Nullable
  public Object beforeBodyWrite(
      @Nullable Object body,
      @NonNull MethodParameter returnType,
      @NonNull MediaType selectedContentType,
      @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
      @NonNull ServerHttpRequest request,
      @NonNull ServerHttpResponse response) {
    // If the body is null and return type is not void, throw 404
    if (body == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    return body;
  }
}
