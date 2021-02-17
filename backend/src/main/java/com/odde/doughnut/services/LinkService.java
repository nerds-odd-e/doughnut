package com.odde.doughnut.services;

import com.odde.doughnut.models.Link;
import com.odde.doughnut.models.Note;
import com.odde.doughnut.repositories.LinkRepository;

import java.util.ArrayList;
import java.util.List;

public class LinkService {

    private LinkRepository _repository;

    public LinkService(LinkRepository repo){
        this._repository = repo;
    }

    public boolean createOneWayLink(Note sourceNote, Note targetNote){
        Link link = new Link();
        link.setSourceNote(sourceNote);
        link.setTargetNote(targetNote);
        Link result = _repository.save(link);

        return result != null;

    }

    public boolean createTwoWayLink(Note sourceNote, Note targetNote){
        Link link = new Link();
        link.setSourceNote(sourceNote);
        link.setTargetNote(targetNote);

        Link linkOpposite = new Link();
        linkOpposite.setSourceNote(targetNote);
        linkOpposite.setTargetNote(sourceNote);

        List<Link> listLink = new ArrayList<Link>();
        listLink.add(link);
        listLink.add(linkOpposite);

        Iterable<Link> result = _repository.saveAll(listLink);

        return result != null && result.iterator().hasNext();

    }
}
