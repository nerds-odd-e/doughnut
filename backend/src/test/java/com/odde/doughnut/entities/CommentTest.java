package com.odde.doughnut.entities;

import com.odde.doughnut.testability.MakeMe;
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

    @Test
    void hasComment() {
        User user = makeMe.aUser().please();
        Note note = makeMe.aNote().please();
        Comment comment = makeMe.aComment(note, user).please();

        assertThat(comment.getId(), is(notNullValue()));
        assertThat(comment.getUser().getId(), equalTo(user.getId()));
        assertThat(comment.getNote().getId(), equalTo(note.getId()));
    }
}
