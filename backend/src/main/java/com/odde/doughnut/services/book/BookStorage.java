package com.odde.doughnut.services.book;

import java.util.Optional;

public interface BookStorage {

  String put(byte[] data);

  Optional<byte[]> get(String ref);
}
