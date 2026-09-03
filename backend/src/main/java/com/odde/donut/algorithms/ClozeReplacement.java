package com.odde.donut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record ClozeReplacement(
    String partialMatchReplacement,
    String fullMatchReplacement,
    String pronunciationReplacement,
    String fullMatchQualifierReplacement) {

  private static final Pattern PRONUNCIATION =
      Pattern.compile(
          "/([^\\s/][^/\\n]*)/(?![a-zA-Z0-9_])",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

  private String maskAliasesAndQualifier(
      String pronunciationMasked, NoteTitle noteTitle, List<TitleFragment> extraAliases) {
    final String internalPartialMatchReplacement = "__p_a_r_t_i_a_l__";
    final String internalFullMatchReplacement = "__f_u_l_l__";
    final String internalFullMatchReplacementForQualifier = "__f_u_l_l_q_u_a_l__";

    var aliases =
        TitleFragment.mergeSortedLongestFirst(noteTitle.getRecallTitleFragments(), extraAliases);
    String step1 =
        replaceFragmentsWithInternalPlaceholder(
            aliases,
            pronunciationMasked,
            (p, t) -> t.replaceLiteralWords(p, internalFullMatchReplacement));
    String step2 =
        replaceFragmentsWithInternalPlaceholder(
            aliases, step1, (p, t) -> t.replaceSimilar(p, internalPartialMatchReplacement));
    String step3 =
        noteTitle
            .getQualifier()
            .map(
                qualifier ->
                    qualifier.replaceLiteralWords(step2, internalFullMatchReplacementForQualifier))
            .orElse(step2);
    return step3
        .replace(internalFullMatchReplacement, fullMatchReplacement)
        .replace(internalPartialMatchReplacement, partialMatchReplacement)
        .replace(internalFullMatchReplacementForQualifier, fullMatchQualifierReplacement);
  }

  private static String replaceFragmentsWithInternalPlaceholder(
      List<TitleFragment> fragments,
      String processed,
      BiFunction<String, TitleFragment, String> replacer) {
    return fragments.stream().reduce(processed, replacer, (x, y) -> y);
  }

  String maskPronunciationsAndTitles(
      String originalContent1,
      List<NoteTitle> noteTitles1,
      List<TitleFragment> extraAliases,
      boolean followsNonWhitespace) {
    final String internalPronunciationReplacement = "__p_r_o_n_u_n_c__";
    final String internalPronunciationSpellingReplacement = "__p_r_o_n_s_p_e_l__";
    // If this segment follows a non-whitespace character in the original HTML,
    // prepend a zero-width marker so suffix patterns can match at segment start
    String contentToProcess = originalContent1;
    if (followsNonWhitespace) {
      contentToProcess = HtmlOrMarkdown.NON_WHITESPACE_CONTEXT_MARKER + originalContent1;
    }
    Matcher matcher = PRONUNCIATION.matcher(contentToProcess);
    StringBuffer pronunciationsReplaced = new StringBuffer();
    List<TitleFragment> pronunciationSpellings = new ArrayList<>();
    while (matcher.find()) {
      if (overlapsWikiLink(contentToProcess, matcher.start(), matcher.end())) {
        matcher.appendReplacement(
            pronunciationsReplaced, Matcher.quoteReplacement(matcher.group(0)));
        continue;
      }
      pronunciationSpellings.add(TitleFragment.from(matcher.group(1)));
      matcher.appendReplacement(
          pronunciationsReplaced, Matcher.quoteReplacement(internalPronunciationReplacement));
    }
    matcher.appendTail(pronunciationsReplaced);
    String spellingRepeatsMasked =
        replaceFragmentsWithInternalPlaceholder(
            TitleFragment.sortedLongestFirst(pronunciationSpellings),
            pronunciationsReplaced.toString(),
            (content, spelling) ->
                spelling.replaceLiteralWords(content, internalPronunciationSpellingReplacement));
    return noteTitles1.stream()
        .reduce(
            spellingRepeatsMasked,
            (content, noteTitle) -> maskAliasesAndQualifier(content, noteTitle, extraAliases),
            (s, s2) -> s)
        .replace(internalPronunciationReplacement, pronunciationReplacement)
        .replace(internalPronunciationSpellingReplacement, fullMatchReplacement)
        .replace(HtmlOrMarkdown.NON_WHITESPACE_CONTEXT_MARKER, "");
  }

  private static boolean overlapsWikiLink(String content, int start, int end) {
    Matcher wiki = WikiLinkMarkdown.INNER_LINK_PATTERN.matcher(content);
    while (wiki.find()) {
      if (start < wiki.end() && end > wiki.start()) {
        return true;
      }
    }
    return false;
  }
}
