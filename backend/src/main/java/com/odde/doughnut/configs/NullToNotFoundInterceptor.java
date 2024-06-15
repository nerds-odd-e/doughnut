package com.odde.doughnut.configs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class NullToNotFoundInterceptor implements HandlerInterceptor {
  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView)
      throws Exception {
    if (handler instanceof HandlerMethod handlerMethod) {
      NullToNotFound nullToNotFoundAnnotation =
          handlerMethod.getMethodAnnotation(NullToNotFound.class);
      if (nullToNotFoundAnnotation != null && modelAndView != null) {
        modelAndView.getModelMap();
        if (modelAndView.getModelMap().isEmpty()) {
          response.sendError(HttpServletResponse.SC_NOT_FOUND, nullToNotFoundAnnotation.message());
        }
      }
    }
  }
}
