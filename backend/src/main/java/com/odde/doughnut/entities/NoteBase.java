package com.odde.doughnut.entities;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.algorithms.HtmlOrMarkdown;
import com.odde.doughnut.algorithms.NoteTitle;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

@MappedSuperclass
public abstract class NoteBase extends Thingy {

  @OneToOne(mappedBy = "note", cascade = CascadeType.ALL)
  @Getter
  @Setter
  @JsonIgnore
  private Thing thing;

  @ManyToOne
  @JoinColumn(name = "notebook_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Notebook notebook;

  @Embedded @Valid @Getter private final NoteAccessories noteAccessories = new NoteAccessories();

  @Column(name = "description")
  @Getter
  @Setter
  @JsonPropertyDescription("The details of the note is in markdown format.")
  private String details;

  @Size(min = 1, max = NoteSimple.MAX_TITLE_LENGTH)
  @Getter
  @Setter
  @Column(name = "topic_constructor")
  private String topicConstructor = "";

  @Column(name = "deleted_at")
  @Getter
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Timestamp deletedAt;

  @OneToMany(mappedBy = "sourceNote")
  @JsonIgnore
  @Getter
  @Setter
  private List<Link> links = new ArrayList<>();

  @OneToMany(mappedBy = "targetNote")
  @JsonIgnore
  @Getter
  @Setter
  private List<Link> refers = new ArrayList<>();

  @OneToMany(mappedBy = "parent", cascade = CascadeType.DETACH)
  @JsonIgnore
  @Where(clause = "deleted_at is null")
  @OrderBy("sibling_order")
  @Getter
  private final List<Note> allChildren = new ArrayList<>();

  public void setDeletedAt(Timestamp value) {
    this.deletedAt = value;
    if (this.thing != null) this.thing.setDeletedAt(value);
  }

  @JsonIgnore
  public NoteTitle getNoteTitle() {
    return new NoteTitle(getTopicConstructor());
  }

  @JsonIgnore
  public List<Note> getSiblings() {
    if (getParent() == null) {
      return new ArrayList<>();
    }
    return getParent().getChildren();
  }

  @JsonIgnore
  public abstract Note getParent();

  @JsonIgnore
  public ClozedString getClozeDescription() {
    if (isDetailsBlankHtml()) return new ClozedString(null, "");

    return ClozedString.htmlClozedString(getDetails()).hide(getNoteTitle());
  }

  @JsonIgnore
  public boolean isDetailsBlankHtml() {
    return new HtmlOrMarkdown(getDetails()).isBlank();
  }

  @JsonIgnore
  public List<Note> getChildren() {
    return getAllChildren().stream()
        .filter(nc -> !nc.usingLinkTypeAsTopicConstructor())
        .collect(toList());
  }

  @JsonIgnore
  public Link.LinkType getLinkType() {
    if (!getTopicConstructor().startsWith(":")) return null;
    return Link.LinkType.fromLabel(getTopicConstructor().substring(1));
  }

  protected boolean usingLinkTypeAsTopicConstructor() {
    return getLinkType() != null;
  }

  protected String getLinkConstructor() {
    if (usingLinkTypeAsTopicConstructor()) {
      Link.LinkType linkType = getLinkType();
      if (linkType == null)
        throw new RuntimeException("Invalid link type: " + getTopicConstructor());
      return "%P is " + linkType.label + " %T";
    }
    return getTopicConstructor();
  }

  @JsonIgnore
  public List<Note> getAncestors() {
    List<Note> result = new ArrayList<>();
    Note p = getParent();
    while (p != null) {
      result.add(0, p);
      p = p.getParent();
    }
    return result;
  }

  @JsonIgnore
  public List<? extends Thingy> getLinkChildren() {
    //    return getAllChildren().stream()
    //        .filter(Note::usingLinkTypeAsTopicConstructor)
    //        .collect(toList());
    return getLinks();
  }
}
