package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ClozeDescriptionTest {
  ClozeReplacement clozeReplacement =
      new ClozeReplacement("[..~]", "[...]", "/.../", "<..~>", "<...>");

  @ParameterizedTest
  @CsvSource({
    "moon,            partner of earth,                    partner of earth",
    "Sedition,        word sedition means this,            word [...] means this",
    "north / up,      it's on the north or up side,        it's on the [...] or [...] side",
    "hort / horticulture,   horticulture is about,         [...] is about",
    "cats/cat,        here is a cat,                       here is a [...]",
    "http://xxx,      xxx,                                 xxx",
    "cats,            a cat,                               a [..~]",
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
    "cattle,          ironcattle,                          iron[...]",
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
    "ignore (complex (brackets)), ignore complex brackets,  ignore complex brackets",
    "cat/dog(animal/weather), dog day is a hot weather,   [...] day is a hot <...>",
    "6,               6year,                               [...]year",
  })
  void clozeDescription(String title, String description, String expectedClozeDescription) {
    assertThat(
        new ClozedString(clozeReplacement, description).hide(new NoteTitle(title)).cloze(),
        equalTo(expectedClozeDescription));
  }

  @Test
  void clozeDescriptionWithMultipleLink() {
    assertThat(
        new ClozedString(clozeReplacement, "a /b\nc/ d").hide(new NoteTitle("title")).cloze(),
        equalTo("a /b\nc/ d"));
  }

  @Test
  void theReplacementsShouldNotInterfereEachOther() {
    ClozeReplacement clozeReplacement =
        new ClozeReplacement("/..~/", "/.../", "(...)", "<.._>", "<...>");
    assertThat(
        new ClozedString(clozeReplacement, "abc").hide(new NoteTitle("abc")).cloze(),
        equalTo("/.../"));
  }
}
