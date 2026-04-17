package com.odde.doughnut.services.book;

public record NotebookBookFile(byte[] bytes, String baseName, String etag, BookFormat format) {}
