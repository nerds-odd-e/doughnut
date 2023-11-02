package com.odde.doughnut.controllers.json;

import lombok.Getter;
import lombok.Setter;

public class UploadFineTuningExamplesResponse {
  @Getter @Setter private boolean isSuccess;
  @Getter @Setter private String message;

  public static UploadFineTuningExamplesResponse fail(String msg) {
    return new UploadFineTuningExamplesResponse() {
      {
        setSuccess(false);
        setMessage(msg);
      }
    };
  }

  public static UploadFineTuningExamplesResponse success() {
    return new UploadFineTuningExamplesResponse() {
      {
        setSuccess(true);
      }
    };
  }
}
