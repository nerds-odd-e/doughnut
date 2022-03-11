package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.entities.json.NotePositionViewedByUser;
import com.odde.doughnut.entities.json.NoteSphere;
import com.odde.doughnut.entities.json.NoteWithPosition;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NoteViewer {

    private User viewer;
    private Note note;

    public NoteViewer(User viewer, Note note) {

        this.viewer = viewer;
        this.note = note;
    }

    public NoteSphere toJsonObject() {
        NoteSphere nvb = new NoteSphere();
        nvb.setId(note.getId());
        nvb.setParentId(note.getParentId());
        nvb.setTitle(note.getTitle());
        nvb.setShortDescription(note.getShortDescription());
        nvb.setNotePicture(note.getNotePicture());
        nvb.setCreatedAt(note.getCreatedAt());
        nvb.setNoteAccessories(note.getNoteAccessories());
        nvb.setLinks(getAllLinks());
        nvb.setChildrenIds(note.getChildren().stream().map(Note::getId).toList());
        nvb.setTextContent(note.getTextContent());

        return nvb;
    }

    public Map<Link.LinkType, LinkViewed> getAllLinks() {
        return Arrays.stream(Link.LinkType.values())
                .map(type->Map.entry(type, new LinkViewed() {{
                    setDirect(linksOfTypeThroughDirect(List.of(type)));
                    setReverse(linksOfTypeThroughReverse(type).collect(Collectors.toList()));
                }}))
                .filter(x -> x.getValue().notEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<Link> linksOfTypeThroughDirect(List<Link.LinkType> linkTypes) {
        return note.getLinks().stream()
                .filter(l -> l.targetVisibleAsSourceOrTo(viewer))
                .filter(l -> linkTypes.contains(l.getLinkType()))
                .collect(Collectors.toList());
    }

    public Stream<Link> linksOfTypeThroughReverse(Link.LinkType linkType) {
        return note.getRefers().stream()
                .filter(l -> l.getLinkType().equals(linkType))
                .filter(l -> l.sourceVisibleAsTargetOrTo(viewer));
    }

    public NotePositionViewedByUser jsonNotePosition(Note note) {
        NotePositionViewedByUser nvb = new NotePositionViewedByUser();
        nvb.setNoteId(note.getId());
        nvb.setTitle(note.getTitle());
        nvb.setNotebook(note.getNotebook());
        nvb.setAncestors(note.getAncestors());
        nvb.setOwns(viewer != null && viewer.owns(note.getNotebook()));
        return nvb;
    }

    public NoteWithPosition jsonNoteWithPosition(Note note) {
        NoteWithPosition nvb = new NoteWithPosition();
        nvb.setNote(toJsonObject());
        nvb.setNotePosition(jsonNotePosition(note));
        return nvb;
    }
}
