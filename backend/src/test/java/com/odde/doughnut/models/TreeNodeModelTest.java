package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class TreeNodeModelTest {
    @Autowired ModelFactoryService modelFactoryService;
    @Autowired MakeMe makeMe;

    NoteEntity topLevel;
    TreeNodeModel treeNodeModel;

    @BeforeEach
    void setup() {
        topLevel = makeMe.aNote().please();
        treeNodeModel = modelFactoryService.toTreeNodeModel(topLevel);
    }

    // This test is leaving empty for demo purpose
    @SuppressWarnings("EmptyMethod")
    @Test
    void shouldGet_0_WhenNoDescendant() {
    }

}