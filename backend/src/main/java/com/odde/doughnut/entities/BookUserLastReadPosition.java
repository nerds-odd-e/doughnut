package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
}
