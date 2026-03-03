package com.odde.doughnut.controllers.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserListingPage {
  private List<UserForListing> users;
  private int pageIndex;
  private int pageSize;
  private long totalCount;

  public int getTotalPages() {
    return (int) Math.ceil((double) totalCount / pageSize);
  }
}
