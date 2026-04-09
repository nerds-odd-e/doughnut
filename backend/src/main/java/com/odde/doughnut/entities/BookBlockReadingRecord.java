package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "book_block_reading_record")
@Getter
@Setter
public class BookBlockReadingRecord extends EntityIdentifiedByIdOnly {

  public static final String STATUS_READ = "READ";

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnore
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_block_id", nullable = false)
  @JsonIgnore
  private BookBlock bookBlock;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(name = "completed_at", nullable = false)
  private Timestamp completedAt;
}
