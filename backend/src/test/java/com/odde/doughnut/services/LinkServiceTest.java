package com.odde.doughnut.services;

import com.odde.doughnut.models.Link;
import com.odde.doughnut.models.Note;
import com.odde.doughnut.repositories.LinkRepository;
import com.odde.doughnut.testability.ApplicationContextWithRepositories;
import com.odde.doughnut.testability.DBCleaner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

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
        Note sourceNote = new Note();
        sourceNote.setTitle("aaa");
        Note targetNote= new Note();
        targetNote.setTitle("bbb");
        svc.createOneWayLink(sourceNote, targetNote);
        ArgumentCaptor<Link> linkCaptor = ArgumentCaptor.forClass(Link.class);
        verify(repo, times(1)).save(linkCaptor.capture());

        List<Link> capturedLink = linkCaptor.getAllValues();
        assertEquals(sourceNote.getTitle(), capturedLink.get(0).getSourceNote().getTitle());
        assertEquals(targetNote.getTitle(), capturedLink.get(0).getTargetNote().getTitle());

    }

    @Test
    void createTwoWayLink() {
        LinkRepository repo = mock(LinkRepository.class);
        LinkService svc = new LinkService(repo);
        Note sourceNote = new Note();
        sourceNote.setTitle("aaa");
        Note targetNote= new Note();
        targetNote.setTitle("bbb");


        svc.createTwoWayLink(sourceNote, targetNote);

        ArgumentCaptor<List<Link>> linkCaptor = ArgumentCaptor.forClass((Class) List.class);

        verify(repo, times(1)).saveAll(linkCaptor.capture());

        List<Link> capturedLink = linkCaptor.getValue();
        assertEquals(sourceNote.getTitle(), capturedLink.get(0).getSourceNote().getTitle());
        assertEquals(targetNote.getTitle(), capturedLink.get(0).getTargetNote().getTitle());

        assertEquals(targetNote.getTitle(), capturedLink.get(1).getSourceNote().getTitle());
        assertEquals(sourceNote.getTitle(), capturedLink.get(1).getTargetNote().getTitle());

    }

}
