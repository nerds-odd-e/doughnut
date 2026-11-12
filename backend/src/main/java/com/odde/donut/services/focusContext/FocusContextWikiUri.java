package com.odde.donut.services.focusContext;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.services.PortablePathAuthoring;
import org.springframework.stereotype.Component;

@Component
public class FocusContextWikiUri {
  private final PortablePathAuthoring portablePathAuthoring;

  public FocusContextWikiUri(PortablePathAuthoring portablePathAuthoring) {
    this.portablePathAuthoring = portablePathAuthoring;
  }

  public String of(Note note, User viewer) {
    return "[[" + portablePathAuthoring.qualifiedPortablePath(note, viewer) + "]]";
  }
}
