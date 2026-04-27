package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Heuristic cloze masking for Japanese dictionary-form titles against conjugated surface forms in
 * recall details: hide the stem, leave a visible ending (e.g. {@code 食べた} → mask+{@code た}).
 */
final class JapaneseLemmaStemMasker {

  private record ConjSpec(String full, String visibleSuffix) {}

  static boolean isEligibleLemma(String lemma) {
    if (lemma == null || lemma.length() < 2) return false;
    if (!lemma.matches("[\\p{IsHan}\\p{IsKatakana}\\p{IsHiragana}]+")) return false;
    String last = lastChar(lemma);
    if (last.equals("い")) return true;
    if (last.equals("る")) return true;
    return isGodanLemmaEnding(last);
  }

  static String maskConjugations(String details, String lemma, String maskToken) {
    List<ConjSpec> specs = buildSpecs(lemma);
    if (specs.isEmpty()) return details;
    specs.sort(Comparator.comparingInt((ConjSpec s) -> s.full.length()).reversed());

    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < details.length()) {
      int cp = details.codePointAt(i);
      if (!leftBoundaryOk(details, i)) {
        out.appendCodePoint(cp);
        i += Character.charCount(cp);
        continue;
      }
      ConjSpec chosen = null;
      for (ConjSpec s : specs) {
        if (i + s.full.length() <= details.length()
            && details.regionMatches(i, s.full, 0, s.full.length())
            && rightBoundaryOk(details, i + s.full.length(), s)) {
          chosen = s;
          break;
        }
      }
      if (chosen != null) {
        out.append(maskToken).append(chosen.visibleSuffix);
        i += chosen.full.length();
      } else {
        out.appendCodePoint(cp);
        i += Character.charCount(cp);
      }
    }
    return out.toString();
  }

  private static List<ConjSpec> buildSpecs(String lemma) {
    String last = lastChar(lemma);
    String body = stripLastChar(lemma);
    if (body.isEmpty()) return List.of();

    Set<String> seen = new LinkedHashSet<>();
    List<ConjSpec> out = new ArrayList<>();

    if (last.equals("い")) {
      addIAdjective(body, out, seen);
      return out;
    }
    if (last.equals("る")) {
      addIchidanRu(body, out, seen);
      addGodanRu(body, out, seen);
      return out;
    }
    if (isGodanLemmaEnding(last)) {
      addGodan(body, last, out, seen);
      return out;
    }
    return out;
  }

  private static void addIAdjective(String stem, List<ConjSpec> out, Set<String> seen) {
    addSpec(stem + "かった", "た", out, seen);
    addSpec(stem + "くない", "ない", out, seen);
    addSpec(stem + "くて", "て", out, seen);
    addSpec(stem + "くなかった", "なかった", out, seen);
    addSpec(stem + "くなければ", "なければ", out, seen);
    addSpec(stem + "ければ", "ば", out, seen);
    addVerbSuffixesOnStem(stem + "く", out, seen);
  }

  private static void addIchidanRu(String stem, List<ConjSpec> out, Set<String> seen) {
    addVerbSuffixesOnStem(stem, out, seen);
  }

  private static void addGodanRu(String body, List<ConjSpec> out, Set<String> seen) {
    addSpec(body + "っ", "た", out, seen);
    addSpec(body + "っ", "て", out, seen);
    addVerbSuffixesOnStem(body + "り", out, seen);
    addSpec(body + "ら", "ない", out, seen);
    addSpec(body + "ら", "なかった", out, seen);
    addSpec(body + "ら", "なければ", out, seen);
    addSpec(body + "ら", "れる", out, seen);
    addSpec(body + "ら", "れます", out, seen);
    addSpec(body + "ら", "れました", out, seen);
    addSpec(body + "ら", "れません", out, seen);
    addSpec(body + "れ", "る", out, seen);
    addSpec(body + "れ", "ます", out, seen);
    addSpec(body + "ろ", "う", out, seen);
  }

  private static void addGodan(String body, String lastKana, List<ConjSpec> out, Set<String> seen) {
    GodanRows r = godanRows(body, lastKana);
    if (r == null) return;

    if (r.mubnuStyle) {
      String nStem = r.iStem.substring(0, r.iStem.length() - 1) + "ん";
      addSpec(nStem + "で", "で", out, seen);
      addSpec(nStem + "だ", "だ", out, seen);
    } else if (r.tsuStyle) {
      addSpec(body + "っ", "て", out, seen);
      addSpec(body + "っ", "た", out, seen);
    } else {
      addSpec(r.teStem + "て", "て", out, seen);
      addSpec(r.teStem + "た", "た", out, seen);
    }
    addVerbSuffixesOnStem(r.iStem, out, seen);
    addSpec(r.aStem + "ない", "ない", out, seen);
    addSpec(r.aStem + "なかった", "なかった", out, seen);
    addSpec(r.aStem + "なければ", "なければ", out, seen);
    addSpec(r.aStem + "れる", "れる", out, seen);
    addSpec(r.aStem + "れます", "ます", out, seen);
    addSpec(r.aStem + "れました", "ました", out, seen);
    addSpec(r.aStem + "れません", "ません", out, seen);

    String potentialVisible = r.eStem.substring(body.length()) + "る";
    addSpec(r.eStem + "る", potentialVisible, out, seen);
    addSpec(r.eStem + "ます", "ます", out, seen);
    addSpec(r.oStem + "う", "う", out, seen);
  }

  private record GodanRows(
      String iStem,
      String aStem,
      String eStem,
      String oStem,
      String teStem,
      boolean tsuStyle,
      boolean mubnuStyle) {}

  private static GodanRows godanRows(String body, String lastKana) {
    return switch (lastKana) {
      case "く" ->
          new GodanRows(body + "き", body + "か", body + "け", body + "こ", body + "い", false, false);
      case "ぐ" ->
          new GodanRows(
              body + "\u304E", body + "が", body + "げ", body + "ご", body + "い", false, false);
      case "す" ->
          new GodanRows(body + "し", body + "さ", body + "せ", body + "そ", body + "し", false, false);
      case "つ" ->
          new GodanRows(body + "ち", body + "た", body + "て", body + "と", body + "", true, false);
      case "る" ->
          new GodanRows(body + "り", body + "ら", body + "れ", body + "ろ", body + "", true, false);
      case "う" ->
          new GodanRows(body + "い", body + "わ", body + "え", body + "お", body + "い", true, false);
      case "む" ->
          new GodanRows(body + "み", body + "ま", body + "め", body + "も", body + "み", false, true);
      case "ぶ" ->
          new GodanRows(
              body + "び", body + "ば", body + "べ", body + "\u307C", body + "び", false, true);
      case "\u306C" ->
          new GodanRows(body + "に", body + "な", body + "ね", body + "の", body + "に", false, true);
      default -> null;
    };
  }

  private static boolean isGodanLemmaEnding(String k) {
    return "\u304F\u3050\u3059\u3064\u306C\u3076\u3080\u3046".contains(k);
  }

  private static void addVerbSuffixesOnStem(String stem, List<ConjSpec> out, Set<String> seen) {
    String[] suffixes = {
      "ませんでした",
      "ません",
      "ましょう",
      "ました",
      "ます",
      "なかった",
      "なければ",
      "なくて",
      "なく",
      "ない",
      "られませんでした",
      "られません",
      "られました",
      "られます",
      "られない",
      "られた",
      "られて",
      "られる",
      "られ",
      "させません",
      "させました",
      "させます",
      "させない",
      "させた",
      "させて",
      "させる",
      "させ",
      "たかった",
      "たくない",
      "たければ",
      "たら",
      "たり",
      "た",
      "て",
      "よう",
    };
    for (String suf : suffixes) {
      addSpec(stem + suf, suf, out, seen);
    }
    // Longer て/た + auxiliary chains (e.g. 熟していない → mask+ていない); must beat bare て/た.
    String[][] teTaChains = {
      {"ていませんでした", "ていませんでした"},
      {"ていません", "ていません"},
      {"ていました", "ていました"},
      {"ています", "ています"},
      {"ていなければ", "ていなければ"},
      {"ていなくて", "ていなくて"},
      {"ていなく", "ていなく"},
      {"ていなかった", "ていなかった"},
      {"ていない", "ていない"},
      {"ていた", "ていた"},
      {"ている", "ている"},
      {"ておきませんでした", "ておきませんでした"},
      {"ておきません", "ておきません"},
      {"ておきました", "ておきました"},
      {"ておきます", "ておきます"},
      {"ておかなかった", "ておかなかった"},
      {"ておかない", "ておかない"},
      {"ておいた", "ておいた"},
      {"ておいて", "ておいて"},
      {"ておく", "ておく"},
      {"てみませんでした", "てみませんでした"},
      {"てみません", "てみません"},
      {"てみました", "てみました"},
      {"てみます", "てみます"},
      {"てみなかった", "てみなかった"},
      {"てみない", "てみない"},
      {"てみた", "てみた"},
      {"てみて", "てみて"},
      {"てみる", "てみる"},
      {"てしまいませんでした", "てしまいませんでした"},
      {"てしまいません", "てしまいません"},
      {"てしまいました", "てしまいました"},
      {"てしまいます", "てしまいます"},
      {"てしまわなかった", "てしまわなかった"},
      {"てしまわない", "てしまわない"},
      {"てしまった", "てしまった"},
      {"てしまって", "てしまって"},
      {"てしまう", "てしまう"},
    };
    for (String[] chain : teTaChains) {
      addSpec(stem + chain[0], chain[1], out, seen);
    }
  }

  private static void addSpec(
      String full, String visibleSuffix, List<ConjSpec> out, Set<String> seen) {
    if (full.length() <= visibleSuffix.length()) return;
    if (!full.endsWith(visibleSuffix)) return;
    if (seen.add(full)) {
      out.add(new ConjSpec(full, visibleSuffix));
    }
  }

  private static boolean leftBoundaryOk(String s, int start) {
    if (start == 0) return true;
    int prev = s.codePointBefore(start);
    Character.UnicodeScript sc = Character.UnicodeScript.of(prev);
    return sc == Character.UnicodeScript.HAN
        || sc == Character.UnicodeScript.HIRAGANA
        || sc == Character.UnicodeScript.KATAKANA;
  }

  private static boolean rightBoundaryOk(String s, int endExclusive, ConjSpec spec) {
    if (endExclusive >= s.length()) return true;
    int cp = s.codePointAt(endExclusive);
    if (Character.isWhitespace(cp)) return true;
    int type = Character.getType(cp);
    if (type == Character.CONNECTOR_PUNCTUATION
        || type == Character.DASH_PUNCTUATION
        || type == Character.START_PUNCTUATION
        || type == Character.END_PUNCTUATION
        || type == Character.OTHER_PUNCTUATION
        || type == Character.INITIAL_QUOTE_PUNCTUATION
        || type == Character.FINAL_QUOTE_PUNCTUATION) {
      return true;
    }
    Character.UnicodeScript sc = Character.UnicodeScript.of(cp);
    if (sc == Character.UnicodeScript.HAN || sc == Character.UnicodeScript.KATAKANA) return true;

    if ("た".equals(spec.visibleSuffix) || "て".equals(spec.visibleSuffix)) {
      // Reject た+い… (desiderative ～たい); allow て+い… (e.g. している, していない).
      if (cp == 'い' && "た".equals(spec.visibleSuffix)) return false;
      if (cp == 'お') return false;
      // ～て/～た + auxiliary (e.g. 試みてみる); next kana is not a particle
      if (sc == Character.UnicodeScript.HIRAGANA) return true;
    }
    return isParticleStart(cp);
  }

  private static boolean isParticleStart(int cp) {
    return "はがをにでのとへもかやねよさ".indexOf(cp) >= 0;
  }

  private static String lastChar(String s) {
    int cp = s.codePointBefore(s.length());
    return new String(Character.toChars(cp));
  }

  private static String stripLastChar(String s) {
    int cp = s.codePointBefore(s.length());
    return s.substring(0, s.length() - Character.charCount(cp));
  }
}
