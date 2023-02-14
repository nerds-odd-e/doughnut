package com.odde.doughnut.testability;

import com.odde.doughnut.testability.builders.*;
import java.nio.CharBuffer;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

@Component
public class MakeMeWithoutDB {
  public String aStringOfLength(int length, char withChar) {
    return CharBuffer.allocate(length).toString().replace('\0', withChar);
  }

  public String aStringOfLength(int length) {
    return aStringOfLength(length, 'a');
  }

  public BindingResult successfulBindingResult() {
    return new FakeBindingResult(false);
  }

  public BindingResult failedBindingResult() {
    return new FakeBindingResult(true);
  }

  public TimestampBuilder aTimestamp() {
    return new TimestampBuilder();
  }

  public UploadedPictureBuilder anUploadedPicture() {
    return new UploadedPictureBuilder();
  }

  public WikidataEntityJsonBuilder wikidataEntityJson() {
    return new WikidataEntityJsonBuilder();
  }

  public WikidataClaimJsonBuilder wikidataClaimsJson(String wikidataId) {
    return new WikidataClaimJsonBuilder(wikidataId);
  }

  public OpenAICompletionResultBuilder openAiCompletionResult() {
    return new OpenAICompletionResultBuilder();
  }
}
