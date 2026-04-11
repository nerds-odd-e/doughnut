package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "book_user_last_read_position")
@Getter
@Setter
public class BookUserLastReadPosition extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnore
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_id", nullable = false)
  @JsonIgnore
  private Book book;

  @Column(name = "page_index", nullable = false)
  private int pageIndex;

  @Column(name = "normalized_y", nullable = false)
  private int normalizedY;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "selected_book_block_id")
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private BookBlock selectedBookBlock;

  @Hidden
  public BookBlock getSelectedBookBlock() {
    return selectedBookBlock;
  }

  @Hidden
  public void setSelectedBookBlock(BookBlock selectedBookBlock) {
    this.selectedBookBlock = selectedBookBlock;
  }

  @JsonProperty("selectedBookBlockId")
  public Integer getSelectedBookBlockId() {
    return selectedBookBlock == null ? null : selectedBookBlock.getId();
  }
}
