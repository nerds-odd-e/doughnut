package com.odde.doughnut.services;

import com.odde.doughnut.controllers.IndexController;
import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.Link;
import com.odde.doughnut.repositories.LinkRepository;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.testability.ApplicationContextWithRepositories;
import com.odde.doughnut.testability.DBCleaner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationContextWithRepositories.class)
@ExtendWith(DBCleaner.class)
public class LinkServiceTest {

    @Test
    void createOneWayLink() {
        LinkRepository repo = mock(LinkRepository.class);
        LinkService svc = new LinkService(repo);
        svc.createOneWayLink(1,2);
        ArgumentCaptor<Link> linkCaptor = ArgumentCaptor.forClass(Link.class);
        verify(repo, times(1)).save(linkCaptor.capture());

        List<Link> capturedLink = linkCaptor.getAllValues();
        assertEquals(1, capturedLink.get(0).getSourceId());
        assertEquals(2, capturedLink.get(0).getTargetId());

    }

    @Test
    void createTwoWayLink() {
        LinkRepository repo = mock(LinkRepository.class);
        LinkService svc = new LinkService(repo);
        svc.createTwoWayLink(1,2);

        ArgumentCaptor<List<Link>> linkCaptor = ArgumentCaptor.forClass((Class) List.class);

        verify(repo, times(1)).saveAll(linkCaptor.capture());

        List<Link> capturedLink = linkCaptor.getValue();
        assertEquals(1, capturedLink.get(0).getSourceId());
        assertEquals(2, capturedLink.get(0).getTargetId());

        assertEquals(2, capturedLink.get(1).getSourceId());
        assertEquals(1, capturedLink.get(1).getTargetId());

    }

}
