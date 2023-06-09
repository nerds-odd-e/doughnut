package com.odde.doughnut.services.openAiApis;

import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;

public class OpenAiAPIImage extends OpenAiApiHandlerBase {

  private OpenAiApi openAiApi;

  public OpenAiAPIImage(OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  public String getOpenAiImage(String prompt) {
    return withExceptionHandler(
        () -> {
          CreateImageRequest completionRequest = getImageRequest(prompt);
          ImageResult choices = getAImage(completionRequest);
          return choices.getData().get(0).getB64Json();
        });
  }

  private CreateImageRequest getImageRequest(String prompt) {
    return CreateImageRequest.builder().prompt(prompt).responseFormat("b64_json").build();
  }

  private ImageResult getAImage(CreateImageRequest completionRequest) {
    return openAiApi.createImage(completionRequest).blockingGet();
  }
}
