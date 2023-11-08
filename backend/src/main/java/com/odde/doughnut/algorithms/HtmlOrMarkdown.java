package com.odde.doughnut.algorithms;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record HtmlOrMarkdown(String htmlOrMarkdown) {
  public String replaceText(Function<String, String> callback) {
    Pattern pattern = Pattern.compile("(?s)(?<=^|>)[^><]+?(?=<|$)");
    Matcher matcher = pattern.matcher(htmlOrMarkdown);
    return matcher.replaceAll(
        matchResult -> Matcher.quoteReplacement(callback.apply(matchResult.group())));
  }

  public boolean isBlank() {
    if (htmlOrMarkdown == null) return true;

    String regexBr = "(\\s*<br[^>]*/?>\\s*)";
    String regex = "^(?:\\s*((<p[^>]*>)(\\s|" + regexBr + ")*</p>\\s*)|" + regexBr + ")*$";

    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(htmlOrMarkdown);

    return matcher.matches();
  }
}
