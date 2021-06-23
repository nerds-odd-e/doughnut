package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.entities.json.LinkViewedByUser;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestLinkControllerTests {
    @Autowired
    ModelFactoryService modelFactoryService;

    @Autowired
    MakeMe makeMe;
    private UserModel userModel;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
    }

    RestLinkController controller() {
        return new RestLinkController(modelFactoryService, new TestCurrentUserFetcher(userModel));
    }

    @Nested
    class showLinkTest {
        User otherUser;
        Note note1;
        Note note2;
        Link link;

        @BeforeEach
        void setup() {
            otherUser = makeMe.aUser().please();
            note1 = makeMe.aNote().byUser(otherUser).please();
            note2 = makeMe.aNote().byUser(otherUser).linkTo(note1).please();
            link = note2.getLinks().get(0);
        }

        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            assertThrows(NoAccessRightException.class, () -> controller().show(link));
        }

        @Test
        void shouldNotBeAbleToSeeItIfICanReadOneNote() throws NoAccessRightException {
            makeMe.aBazaarNodebook(note1.getNotebook()).please();
            assertThrows(NoAccessRightException.class, () -> controller().show(link));
        }

        @Test
        void shouldBeAbleToSeeItIfICanReadBothNote() throws NoAccessRightException {
            makeMe.aBazaarNodebook(note1.getNotebook()).please();
            makeMe.aBazaarNodebook(note2.getNotebook()).please();
            LinkViewedByUser linkViewedByUser = controller().show(link);
            assertThat(linkViewedByUser.getReadonly(), equalTo(true));

        }

    }

    @Nested
    class showLinkStatisticsTest {
        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            User otherUser = makeMe.aUser().please();
            Note note1 = makeMe.aNote().byUser(otherUser).please();
            Note note2 = makeMe.aNote().byUser(otherUser).linkTo(note1).please();
            Link link = note2.getLinks().get(0);
            assertThrows(NoAccessRightException.class, () -> controller().statistics(link));
        }
    }

}
