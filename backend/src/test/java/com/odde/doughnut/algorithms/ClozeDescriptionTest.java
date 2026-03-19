package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClozeDescriptionTest {
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
    "～かたわら,        彼女は猫を可愛がる*かたわら*、犬に対してはなぜか冷たい。,  [...]",
  })
  void clozeDescription(String title, String details, String expectedClozeDescription) {
    assertThat(
        new ClozedString(clozeReplacement, details)
            .hide(new NoteTitle(title))
            .maskedDetailsAsMarkdown(),
        containsString(expectedClozeDescription));
  }

  @Test
  void clozeDescriptionWithMultipleLink() {
    assertThat(
        new ClozedString(clozeReplacement, "a /b\nc/ d")
            .hide(new NoteTitle("title"))
            .maskedDetailsAsMarkdown(),
        containsString("a /b\nc/ d"));
  }

  @Test
  void shouldAvoidTheDollarSignBug() {
    assertThat(
        new ClozedString(clozeReplacement, "$2")
            .hide(new NoteTitle("Stable Diffusion"))
            .maskedDetailsAsMarkdown(),
        containsString("$2"));
  }

  @Test
  void theReplacementsShouldNotInterfereEachOther() {
    ClozeReplacement clozeReplacement = new ClozeReplacement("/..~/", "/.../", "(...)", "<...>");
    assertThat(
        new ClozedString(clozeReplacement, "abc")
            .hide(new NoteTitle("abc"))
            .maskedDetailsAsMarkdown(),
        containsString("/.../"));
  }

  @Test
  void clozeShouldWorkWithSlashInTitleAndUrlsInDetails() {
    String title = "archenemy / arch-enemy";
    String details =
        "In literature, an **archenemy** (sometimes spelled as **arch-enemy**) or **nemesis** is the main [enemy](https://en.wikipedia.org/wiki/Enemy) of the [protagonist](https://en.wikipedia.org/wiki/Protagonist)—or sometimes, one of the other main characters—appearing as the most prominent and most-known enemy of the [hero](https://en.wikipedia.org/wiki/Hero)";
    String result =
        new ClozedString(clozeReplacement, details)
            .hide(new NoteTitle(title))
            .maskedDetailsAsMarkdown();

    // The word "archenemy" and "arch-enemy" should be clozed
    assertThat(result, containsString("[...]"));

    // URLs should not be affected
    assertThat(result, containsString("https://en.wikipedia.org/wiki/Enemy"));
    assertThat(result, containsString("https://en.wikipedia.org/wiki/Protagonist"));
    assertThat(result, containsString("https://en.wikipedia.org/wiki/Hero"));
  }

  @Test
  void clozeShouldMaskPronunciationFollowedByJapaneseParticle() {
    String details =
        "/あしかが よしみつ/は、室町時代前期の室町幕府第3代将軍（在職：1369年 - 1395年）である。";
    String result =
        new ClozedString(clozeReplacement, details)
            .hide(new NoteTitle("足利義満"))
            .maskedDetailsAsMarkdown();
    assertThat(result, containsString("/.../"));
  }

  @ParameterizedTest
  @CsvSource({
    "archenemy / arch-enemy, an archenemy here, an [...] here",
    "archenemy / arch-enemy, an arch-enemy here, an [...] here",
    "archenemy / arch-enemy, the archenemy and arch-enemy are, the [...] and [...] are",
  })
  void clozeShouldHandleTitleWithSlash(String title, String details, String expected) {
    String result =
        new ClozedString(clozeReplacement, details)
            .hide(new NoteTitle(title))
            .maskedDetailsAsMarkdown();
    assertThat(result, containsString(expected));
  }
}
