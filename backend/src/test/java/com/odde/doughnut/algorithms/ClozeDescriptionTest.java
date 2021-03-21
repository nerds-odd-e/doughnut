package com.odde.doughnut.algorithms;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ClozeDescriptionTest {

    @ParameterizedTest
    @CsvSource({
            "moon,            partner of earth,                    partner of earth",
            "Sedition,        word sedition means this,            word [...] means this",
            "north / up,      it's on the north or up side,        it's on the [...] or [...] side",
            "cats,            a cat,                               a [..~]",
            "cat-dog,         cat dog,                             [...]",
            "cat dog,         cat-dog,                             [...]",
            "cat dog,         cat and dog,                         [...]",
            "cat dog,         cat a dog,                           [...]",
            "cat dog,         cat the dog,                         [...]",
            "cat the dog,     cat dog,                             [...]",
            "cat,             /kat/,                               /.../",
    })
    void clozeDescription(String title, String description, String expectedClozeDescription) {
        ClozeDescription clozeDescription = new ClozeDescription();
        assertThat(clozeDescription.getClozeDescription(title, description), equalTo(expectedClozeDescription));
    }
}
