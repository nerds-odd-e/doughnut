package com.odde.doughnut.services.book;

import org.springframework.http.MediaType;

public record NotebookBookFile(
    byte[] bytes, String attachmentFileName, String etag, MediaType contentType) {}
