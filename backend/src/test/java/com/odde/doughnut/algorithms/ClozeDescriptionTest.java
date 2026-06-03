package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClozeDescriptionTest {
  ClozeReplacement clozeReplacement = new ClozeReplacement("[..~]", "[...]", "/.../", "<...>");

  @ParameterizedTest
  @CsvSource({
    "moon,            partner of earth,                    partner of earth",
    "Sedition,        word sedition means this,            word [...] means this",
    "north／up,      it's on the north or up side,        it's on the [...] or [...] side",
    "hort／horticulture,   horticulture is about,         [...] is about",
    "cats／cat,        here is a cat,                       here is a [...]",
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
    "cat／dog(animal／weather), dog day is a hot weather,   [...] day is a hot <...>",
    "6,               6year,                              [...]year",
    "cat,             <p class='cat'>a cat</p>,           <p class='cat'>a [...]</p>",
    "～かたわら,        彼女は猫を可愛がる*かたわら*、犬に対してはなぜか冷たい。,  [...]",
    "食べる,            昨日食べた,                          昨日[..~]た",
    "鳴く,      鳴いて,                    [..~]て",
    "鳴く,      鳴ける,                    [..~]ける",
    "鳴く,      鳴かれる,                [..~]れる",
    "鳴く,      鳴きます,                [..~]ます",
    "高い,      高かった,                [..~]た",
    "高い,      高くない,                [..~]ない",
    "高い,      今日は高そうだ,           今日は[..~]そうだ",
    "高い,      高さが大切,              [..~]さが大切",
    "高い,      家賃が高すぎる,          家賃が[..~]る",
    "高い,      高すぎて困る,            [..~]て困る",
    "高い,      気温が高くなる,          気温が[..~]なる",
    "高い,      物価が高くなった,        物価が[..~]なった",
    "高い,      リスクは高くありません,   リスクは[..~]ありません",
    "高い,      もはや高くありませんでした, もはや[..~]ありませんでした",
    "試みる,   わたしが試みてみよう。,  わたしが[..~]てみよう。",
    "熟す,      この語はまだ熟していない。,  この語はまだ[..~]ていない。",
    "bona fide, _Bona fides_ is a Latin phrase meaning \"good faith\"., [...]",
  })
  void clozeDescription(String title, String markdown, String expectedClozeDescription) {
    assertThat(
        new ClozedString(clozeReplacement, markdown)
            .hide(new NoteTitle(title))
            .maskedContentAsMarkdown(),
        containsString(expectedClozeDescription));
  }

  @Test
  void clozeDescriptionWithMultipleLink() {
    assertThat(
        new ClozedString(clozeReplacement, "a /b\nc/ d")
            .hide(new NoteTitle("title"))
            .maskedContentAsMarkdown(),
        containsString("a /b\nc/ d"));
  }

  @Test
  void shouldAvoidTheDollarSignBug() {
    assertThat(
        new ClozedString(clozeReplacement, "$2")
            .hide(new NoteTitle("Stable Diffusion"))
            .maskedContentAsMarkdown(),
        containsString("$2"));
  }

  @Test
  void theReplacementsShouldNotInterfereEachOther() {
    ClozeReplacement clozeReplacement = new ClozeReplacement("/..~/", "/.../", "(...)", "<...>");
    assertThat(
        new ClozedString(clozeReplacement, "abc")
            .hide(new NoteTitle("abc"))
            .maskedContentAsMarkdown(),
        containsString("/.../"));
  }

  @Test
  void clozeShouldWorkWithSlashInTitleAndUrlsInContent() {
    String title = "archenemy／arch-enemy";
    String markdown =
        "In literature, an **archenemy** (sometimes spelled as **arch-enemy**) or **nemesis** is the main [enemy](https://en.wikipedia.org/wiki/Enemy) of the [protagonist](https://en.wikipedia.org/wiki/Protagonist)—or sometimes, one of the other main characters—appearing as the most prominent and most-known enemy of the [hero](https://en.wikipedia.org/wiki/Hero)";
    String result =
        new ClozedString(clozeReplacement, markdown)
            .hide(new NoteTitle(title))
            .maskedContentAsMarkdown();

    // The word "archenemy" and "arch-enemy" should be clozed
    assertThat(result, containsString("[...]"));

    // URLs should not be affected
    assertThat(result, containsString("https://en.wikipedia.org/wiki/Enemy"));
    assertThat(result, containsString("https://en.wikipedia.org/wiki/Protagonist"));
    assertThat(result, containsString("https://en.wikipedia.org/wiki/Hero"));
  }

  @Test
  void clozeShouldMaskPronunciationBeforeMarkdownBlockquote() {
    String markdown =
        """
        「多大\u3000/ただい/」とは、数や量。

        > **例文:**<br>
        > 「お客様に**多大なる**ご迷惑を」
        """;
    String result =
        new ClozedString(clozeReplacement, markdown)
            .hide(new NoteTitle("多大"))
            .maskedContentAsMarkdown();
    assertThat(result, not(containsString("/ただい/")));
    assertThat(result, containsString("/.../"));
  }

  @Test
  void clozeShouldMaskPronunciationFollowedByJapaneseParticle() {
    String markdown = "/あしかが よしみつ/は、室町時代前期の室町幕府第3代将軍（在職：1369年 - 1395年）である。";
    String result =
        new ClozedString(clozeReplacement, markdown)
            .hide(new NoteTitle("足利義満"))
            .maskedContentAsMarkdown();
    assertThat(result, containsString("/.../"));
  }

  @Test
  void clozeShouldFullyMaskJapaneseTitleFollowedByParticle() {
    String markdown = "如何，怎样。（どんなふう。） どのようなぐあいですか。／情况如何？ どのように致しましょうか。／应该怎样做？";
    String result =
        new ClozedString(clozeReplacement, markdown)
            .hide(new NoteTitle("どのよう"))
            .maskedContentAsMarkdown();
    assertThat(result, containsString("[...]"));
    assertThat(result, containsString("[...]なぐあいですか"));
    assertThat(result, containsString("[...]に致しましょうか"));
    assertThat(result, not(containsString("うなぐあい")));
    assertThat(result, not(containsString("うに致し")));
  }

  @Test
  void clozeShouldNotMatchPartialWordInDogma() {
    String result =
        new ClozedString(clozeReplacement, "the cat dogma is a belief")
            .hide(new NoteTitle("cat dog"))
            .maskedContentAsMarkdown();
    assertThat(result, containsString("dogma"));
  }

  @Test
  void clozeShouldMaskEvictedWhenTitleIsEvict() {
    String result =
        new ClozedString(
                clozeReplacement,
                "a single mother and her children have been **evicted from** their home")
            .hide(new NoteTitle("evict"))
            .maskedContentAsMarkdown();
    assertThat(result, containsString("[...]"));
  }

  @ParameterizedTest
  @CsvSource({
    "archenemy／arch-enemy, an archenemy here, an [...] here",
    "archenemy／arch-enemy, an arch-enemy here, an [...] here",
    "archenemy／arch-enemy, the archenemy and arch-enemy are, the [...] and [...] are",
  })
  void clozeShouldHandleTitleWithAlternativeSeparator(
      String title, String markdown, String expected) {
    String result =
        new ClozedString(clozeReplacement, markdown)
            .hide(new NoteTitle(title))
            .maskedContentAsMarkdown();
    assertThat(result, containsString(expected));
  }
}
