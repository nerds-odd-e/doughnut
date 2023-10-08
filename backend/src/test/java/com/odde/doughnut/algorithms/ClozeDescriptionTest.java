package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ClozeDescriptionTest {
  ClozeReplacement clozeReplacement = new ClozeReplacement("[..~]", "[...]", "/.../", "<...>");

  @ParameterizedTest
  @CsvSource({
    "moon,            partner of earth,                    partner of earth",
    "Sedition,        word sedition means this,            word [...] means this",
    "north / up,      it's on the north or up side,        it's on the [...] or [...] side",
    "hort / horticulture,   horticulture is about,         [...] is about",
    "cats/cat,        here is a cat,                       here is a [...]",
    "http://xxx,      xxx,                                 xxx",
    "cats,            a cat,                               a [..~]",
    "istio,           existing,                            existing",
    "cat-dog,         cat dog,                             [...]",
    "cat dog,         cat-dog,                             [...]",
    "cat dog,         cat and dog,                         [...]",
    "cat dog,         cat a dog,                           [...]",
    "cat dog,         cat the dog,                         [...]",
    "cat the dog,     cat dog,                             [...]",
    "cat,             concat,                              concat",
    "avid,            having,                              having",
    "よう,             どのよう,                              どの[...]",
    "cat,             /kat/,                               /.../",
    "cat,             (/kat/),                             (/.../)",
    "cat,             http://xxx/ooo,                      http://xxx/ooo",
    "cat,             moody / narcissism / apathetic,      moody / narcissism / apathetic",
    "t,               the t twins,                         the [...] twins",
    "t,               (t),                                 ([...])",
    "鳴く,             羊はなんて鳴くの？,                     羊はなんて[...]の？",
    "鳴く,             羊鳴く,                               羊[...]",
    "鳴く,             ネコ鳴く,                             ネコ[...]",
    "cattle,          ironcattle,                          ironcattle",
    "~cato,           concato,                             con[...]",
    "~cat,            concat,                              con[...]",
    "~cat,            cat,                                 cat",
    "~cat,            a cat,                               a cat",
    "〜よう,            どのよう,                            どの[...]",
    "にとっては,       「にとっては」と,                      「[...]」と",
    "〜にとっては,       「〜にとっては」と,                    「〜[...]」と",
    "～によると／によれば,  名詞＋によると　名詞＋によれば,        名詞＋[...]　名詞＋[...]",
    "cat(animal),      cat is an animal,                  [...] is an <...>",
    "cat（animal),      cat is an animal,                  [...] is an <...>",
    "cat(animal) dog,  cat is an animal,                  cat is an animal",
    "「いい和悪い」,      然后,                               然后",
    "олет,             Это самолет,                        Это самолет",
    "不客气,            😃不客气,                           😃[...]",
    "ignore (complex (brackets)), ignore complex brackets,  ignore complex brackets",
    "cat/dog(animal/weather), dog day is a hot weather,   [...] day is a hot <...>",
    "6,               6year,                              [...]year",
    "cat,             <p class='cat'>a cat</p>,           <p class='cat'>a [...]</p>",
  })
  void clozeDescription(String title, String details, String expectedClozeDescription) {
    assertThat(
        new ClozedString(clozeReplacement, details).hide(new NoteTitle(title)).clozeDetails(),
        containsString(expectedClozeDescription));
  }

  @Disabled
  @ParameterizedTest
  @CsvSource({
    "moon,     partner of earth,            partner of earth",
    "sedition, word sedition means this,    word [...] means this",
    "sting,    existing,                    existing",
    "鳴く,     羊はなんて鳴くの？,              羊はなんて[...]の？",
    "rapport,  你知道rapport是什么吗？,        你知道[…]是什么吗？",
    "олет,     Это самолет,                 Это самолет",
    "不客气,    😃不客气,                     😃[...]",
  })
  void clozeDescriptionTest(String token, String description, String expectedClozeDescription) {
    String regex = "(?:(?<=[^\\w-])" + Pattern.quote(token) + "(?=[^\\w-]))";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(description);
    String result = matcher.replaceAll("[...]");
    assertThat(result, equalTo(expectedClozeDescription));
  }

  @Test
  void clozeDescriptionWithMultipleLink() {
    assertThat(
        new ClozedString(clozeReplacement, "a /b\nc/ d")
            .hide(new NoteTitle("title"))
            .clozeDetails(),
        containsString("a /b\nc/ d"));
  }

  @Test
  void shouldAvoidTheDollarSignBug() {
    assertThat(
        new ClozedString(clozeReplacement, "$2")
            .hide(new NoteTitle("Stable Diffusion"))
            .clozeDetails(),
        containsString("$2"));
  }

  @Test
  void theReplacementsShouldNotInterfereEachOther() {
    ClozeReplacement clozeReplacement = new ClozeReplacement("/..~/", "/.../", "(...)", "<...>");
    assertThat(
        new ClozedString(clozeReplacement, "abc").hide(new NoteTitle("abc")).clozeDetails(),
        containsString("/.../"));
  }
}
