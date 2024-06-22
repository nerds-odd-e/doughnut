package com.odde.doughnut.testability;

import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

class ResponseBodyCallStub implements Call<ResponseBody> {
  private final ResponseBody responseBody;

  public ResponseBodyCallStub(ResponseBody responseBody) {
    this.responseBody = responseBody;
  }

  @Override
  public Response<ResponseBody> execute() {
    return Response.success(responseBody);
  }

  @Override
  public void enqueue(Callback<ResponseBody> callback) {
    callback.onResponse(this, Response.success(responseBody));
  }

  @Override
  public boolean isExecuted() {
    return true;
  }

  @Override
  public void cancel() {
    // No-op
  }

  @Override
  public boolean isCanceled() {
    return false;
  }

  @Override
  public Call<ResponseBody> clone() {
    return this;
  }

  @Override
  public Request request() {
    return new Request.Builder().url("http://localhost").build();
  }

  @Override
  public Timeout timeout() {
    return null;
  }
}
