package com.odde.doughnut.services.book;

import java.util.Optional;

public interface BookPdfStorage {

  String put(byte[] data);

  Optional<byte[]> get(String ref);
}
