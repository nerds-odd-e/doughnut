package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.entities.json.NoteViewedByUser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NoteViewer {

    private User viewer;
    private Note note;

    public NoteViewer(User viewer, Note note) {

        this.viewer = viewer;
        this.note = note;
    }

    public NoteViewedByUser toJsonObject() {
        NoteViewedByUser nvb = new NoteViewedByUser();
        nvb.setId(note.getId());
        nvb.setParentId(note.getParentId());
        nvb.setTitle(note.getTitle());
        nvb.setShortDescription(note.getShortDescription());
        nvb.setNotePicture(note.getNotePicture());
        nvb.setCreatedAt(note.getCreatedAt());
        nvb.setNoteContent(note.getNoteContent());
        nvb.setLinks(getAllLinks());
        nvb.setChildrenIds(note.getChildren().stream().map(Note::getId).collect(Collectors.toUnmodifiableList()));
        return nvb;
    }

    public Map<Link.LinkType, LinkViewed> getAllLinks() {
        return Arrays.stream(Link.LinkType.values())
                .map(type->Map.entry(type, new LinkViewed() {{
                    setDirect(linksOfTypeThroughDirect(List.of(type)));
                            setReverse(note.linksOfTypeThroughReverse(type, viewer).collect(Collectors.toList()));
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
}
