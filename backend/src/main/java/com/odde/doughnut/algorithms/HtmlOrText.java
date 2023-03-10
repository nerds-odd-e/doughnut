package com.odde.doughnut.algorithms;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.util.Strings;

public record HtmlOrText(String htmlOrText) {
  public String replaceText(Function<String, String> callback) {
    Pattern pattern = Pattern.compile("(?s)(?<=^|>)[^><]+?(?=<|$)");
    Matcher matcher = pattern.matcher(htmlOrText.replace("$", "\\$"));
    return matcher.replaceAll(
        matchResult -> {
          String text = matchResult.group();
          return callback.apply(text);
        });
  }

  public boolean isBlank() {
    return Strings.isEmpty(this.htmlOrText);
  }
}
