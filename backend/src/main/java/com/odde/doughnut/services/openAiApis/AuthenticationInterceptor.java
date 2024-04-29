package com.odde.doughnut.services.openAiApis;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;

public class AuthenticationInterceptor implements Interceptor {
  private final String token;

  AuthenticationInterceptor(String token) {
    Objects.requireNonNull(token, "OpenAI token required");
    this.token = token;
  }

  public Response intercept(Interceptor.Chain chain) throws IOException {
    Request request = chain.request().newBuilder().header("Authorization", "Bearer " + this.token).build();
    return chain.proceed(request);
  }
}
