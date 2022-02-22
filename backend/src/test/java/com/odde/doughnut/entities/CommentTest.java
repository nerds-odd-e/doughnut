package com.odde.doughnut.entities;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class CommentTest {
    @Autowired
    MakeMe makeMe;
    User user;
    Note note;

    @BeforeEach
    void setup() {
        user = makeMe.aUser().please();
        note = makeMe.aNote().please();
    }

    @Test
    void shouldCreateComment() {
        Comment comment = makeMe.aComment(note, user).please();

        assertThat(comment.getId(), is(notNullValue()));
        assertThat(comment.getUser().getId(), equalTo(user.getId()));
        assertThat(comment.getNote().getId(), equalTo(note.getId()));
    }

    @Test
    void shouldCreateCommentWithContent() {
        String content = "My comment";
        Comment comment = makeMe.aComment(note, user, content).please();

        assertThat(comment.getContent(), equalTo(content));
    }
}
