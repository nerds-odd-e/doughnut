package com.odde.doughnut.exceptions;

import com.odde.doughnut.controllers.json.ApiError;
import org.springframework.http.HttpStatus;

public class WikidataServiceErrorException extends ApiException {
  public WikidataServiceErrorException(String message, HttpStatus status) {
    super(
        message,
        ApiError.ErrorType.WIKIDATA_SERVICE_ERROR,
        "Wikidata service error, with status: " + status.name());
  }
}
