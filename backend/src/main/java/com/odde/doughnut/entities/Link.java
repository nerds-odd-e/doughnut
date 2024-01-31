package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "link")
@JsonPropertyOrder({"clozeSource", "linkTypeLabel"})
public class Link extends EntityIdentifiedByIdOnly {
  public Link() {}

  @ManyToOne
  @JoinColumn(name = "source_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note sourceNote;

  @ManyToOne
  @JoinColumn(name = "target_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note targetNote;

  @NotNull
  @Column(name = "type_id")
  @Getter
  @Setter
  @JsonIgnore
  private Integer typeId;

  public LinkType getLinkType() {
    return LinkType.fromId(typeId);
  }

  public void setLinkType(LinkType linkType) {
    if (linkType == null) {
      typeId = null;
      return;
    }
    typeId = linkType.id;
  }
}
