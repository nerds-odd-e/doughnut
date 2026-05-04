package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApproximateUtf8TokenBudgetTest {

  @Test
  void utf8CharactersConsumeMoreBytesThanAscii() {
    String ascii = "hello world ".repeat(10);
    String cjk = "你好世界 ".repeat(10);

    int asciiLen = ApproximateUtf8TokenBudget.truncateByApproxTokens(ascii, 1000).length();
    int cjkLen = ApproximateUtf8TokenBudget.truncateByApproxTokens(cjk, 1000).length();

    assertTrue(
        cjkLen < asciiLen,
        () ->
            String.format(
                "CJK should truncate shorter at same token cap; ascii=%d cjk=%d",
                asciiLen, cjkLen));
  }
}
