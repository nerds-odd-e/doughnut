package com.odde.donut.services.book;

import java.util.Optional;

public interface BookStorage {

  String put(byte[] data, String format);

  Optional<byte[]> get(String ref);

  void delete(String ref);
}
