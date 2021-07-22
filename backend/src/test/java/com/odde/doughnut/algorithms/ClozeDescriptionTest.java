package com.odde.doughnut.algorithms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ClozeDescriptionTest {
    ClozeDescription clozeDescription = new ClozeDescription(
            "[..~]",
            "[...]",
            "/.../",
            "<..~>",
            "<...>");

    @ParameterizedTest
    @CsvSource({
            "moon,            partner of earth,                    partner of earth",
            "Sedition,        word sedition means this,            word [...] means this",
            "north / up,      it's on the north or up side,        it's on the [...] or [...] side",
            "http://xxx,      xxx,                                 xxx",
            "cats,            a cat,                               a [..~]",
            "cat-dog,         cat dog,                             [...]",
            "cat dog,         cat-dog,                             [...]",
            "cat dog,         cat and dog,                         [...]",
            "cat dog,         cat a dog,                           [...]",
            "cat dog,         cat the dog,                         [...]",
            "cat the dog,     cat dog,                             [...]",
            "cat,             concat,                              concat",
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
            "〜よう,            どのよう,                              どの[...]",
            "cat(animal),      cat is an animal,                  [...] is an <...>",
            "cat(animal) dog,  cat is an animal,                  cat is an animal",
            "cat/dog(animal/weather), dog day is a hot weather,   [...] day is a hot <...>",
            "6,               6year,                               [...]year",
    })
    void clozeDescription(String title, String description, String expectedClozeDescription) {
        assertThat(clozeDescription.getClozeDescription(new NoteTitle(title), description), equalTo(expectedClozeDescription));
    }

    @Test
    void clozeDescriptionWithMultipleLink() {
        assertThat(clozeDescription.getClozeDescription(new NoteTitle("title"), "a /b\nc/ d"), equalTo("a /b\nc/ d"));
    }
    @Test
    void theReplacementsShouldNotInterfereEachOther() {
        ClozeDescription clozeDescription = new ClozeDescription(
                "/..~/",
                "/.../",
                "(...)", "<.._>", "<...>");
        assertThat(clozeDescription.getClozeDescription(new NoteTitle("abc"), "abc"), equalTo("/.../"));
    }

}
