package com.odde.doughnut.services;

import com.odde.doughnut.models.Link;
import com.odde.doughnut.repositories.LinkRepository;

import javax.swing.text.html.parser.Entity;
import java.util.ArrayList;
import java.util.List;

public class LinkService {

    private LinkRepository _repository;

    public LinkService(LinkRepository repo){
        this._repository = repo;
    }

    public boolean createOneWayLink(int sourceId, int targetId){
        Link link = new Link();
        link.setSourceId(sourceId);
        link.setTargetId(targetId);
        Link result = _repository.save(link);

        return result != null;

    }

    public boolean createTwoWayLink(int sourceId, int targetId){
        Link link = new Link();
        link.setSourceId(sourceId);
        link.setTargetId(targetId);

        Link linkOpposite = new Link();
        linkOpposite.setSourceId(targetId);
        linkOpposite.setTargetId(sourceId);

        List<Link> listLink = new ArrayList<Link>();
        listLink.add(link);
        listLink.add(linkOpposite);

        Iterable<Link> result = _repository.saveAll(listLink);

        return result != null && result.iterator().hasNext();

    }
}
