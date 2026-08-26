package com.odde.donut.exceptions;

import com.odde.donut.controllers.dto.ApiError;
import org.springframework.http.HttpStatus;

public class WikidataServiceErrorException extends ApiException {
  public WikidataServiceErrorException(String message, HttpStatus status) {
    super(message, ApiError.ErrorType.WIKIDATA_SERVICE_ERROR, message);
  }
}
