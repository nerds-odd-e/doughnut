package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "book_content_block")
public class BookContentBlock extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_block_id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private BookBlock bookBlock;

  @Column(name = "sibling_order", nullable = false)
  @Getter
  @Setter
  private int siblingOrder;

  @Column(name = "type", nullable = false, length = 128)
  @Getter
  @Setter
  private String type;

  @Column(name = "page_idx")
  @Getter
  @Setter
  private Integer pageIdx;

  @Column(name = "raw_data", nullable = false, columnDefinition = "LONGTEXT")
  @Getter
  @Setter
  private String rawData;
}
