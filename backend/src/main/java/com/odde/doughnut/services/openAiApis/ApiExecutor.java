package com.odde.doughnut.services.openAiApis;

import static com.theokanning.openai.service.OpenAiService.execute;

import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.theokanning.openai.OpenAiApi;
import io.reactivex.Single;
import java.net.SocketTimeoutException;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import retrofit2.HttpException;

public record ApiExecutor(OpenAiApi openAiApi) {
  public <T> T exec(Function<OpenAiApi, Single<T>> callable) {
    try {
      return execute(callable.apply(openAiApi));
    } catch (HttpException e) {
      if (HttpStatus.UNAUTHORIZED.value() == e.code()) {
        throw new OpenAiUnauthorizedException(e.getMessage());
      }
      if (e.code() / 100 == 5) {
        throw new OpenAIServiceErrorException(e.getMessage(), HttpStatus.valueOf(e.code()));
      }
      System.out.println(e.message());
      throw e;
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      if (cause instanceof SocketTimeoutException) {
        throw new OpenAITimeoutException(cause.getMessage());
      }
      throw e;
    }
  }
}
