package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class ApproximateUtf8TokenBudgetTest {

  @Test
  void utf8CharactersConsumeMoreBytesThanAscii() {
    String ascii = "hello world ".repeat(10);
    String cjk = "你好世界 ".repeat(10);

    int asciiLen = ApproximateUtf8TokenBudget.truncateByApproxTokens(ascii, 1000).length();
    int cjkLen = ApproximateUtf8TokenBudget.truncateByApproxTokens(cjk, 1000).length();

    assertThat(cjkLen, lessThan(asciiLen));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void estimateApproxTokensIsZeroForBlank(String input) {
    assertThat(ApproximateUtf8TokenBudget.estimateApproxTokens(input), equalTo(0));
  }

  @Test
  void estimateApproxTokensIsOneForSingleChar() {
    assertThat(ApproximateUtf8TokenBudget.estimateApproxTokens("x"), equalTo(1));
  }
}
