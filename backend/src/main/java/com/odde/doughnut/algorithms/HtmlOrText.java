package com.odde.doughnut.algorithms;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlOrText {
  private String htmlOrText;

  public HtmlOrText(String htmlOrText) {
    this.htmlOrText = htmlOrText;
  }

  public String replaceText(Function<String, String> callback) {
    Pattern pattern = Pattern.compile("(?s)(?<=^|>)[^><]+?(?=<|$)");
    Matcher matcher = pattern.matcher(htmlOrText.replace("$", "\\$"));
    return matcher.replaceAll(
        matchResult -> {
          String text = matchResult.group();
          return callback.apply(text);
        });
  }
}
