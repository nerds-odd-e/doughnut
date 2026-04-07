package com.odde.doughnut.entities;

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
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @Column(name = "page_index", nullable = false)
  private int pageIndex;

  @Column(name = "normalized_y", nullable = false)
  private int normalizedY;
}
