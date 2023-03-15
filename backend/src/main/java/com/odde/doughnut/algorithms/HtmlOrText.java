package com.odde.doughnut.algorithms;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record HtmlOrText(String htmlOrText) {
  public String replaceText(Function<String, String> callback) {
    Pattern pattern = Pattern.compile("(?s)(?<=^|>)[^><]+?(?=<|$)");
    Matcher matcher = pattern.matcher(htmlOrText);
    return matcher.replaceAll(
        matchResult -> Matcher.quoteReplacement(callback.apply(matchResult.group())));
  }

  public boolean isBlank() {
    if (htmlOrText == null) return true;

    String regexBr = "(\\s*<br[^>]*\\/?>\\s*)";
    String regex = "^(?:\\s*((<p[^>]*>)(\\s|" + regexBr + ")*<\\/p>\\s*)|" + regexBr + ")*$";

    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(htmlOrText);

    return matcher.matches();
  }
}
