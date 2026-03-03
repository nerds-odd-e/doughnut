package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.UserForListing;
import com.odde.doughnut.controllers.dto.UserListingPage;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
class AdminUserController {
  private final AuthorizationService authorizationService;
  private final UserRepository userRepository;
  private final NoteRepository noteRepository;
  private final MemoryTrackerRepository memoryTrackerRepository;

  public AdminUserController(
      AuthorizationService authorizationService,
      UserRepository userRepository,
      NoteRepository noteRepository,
      MemoryTrackerRepository memoryTrackerRepository) {
    this.authorizationService = authorizationService;
    this.userRepository = userRepository;
    this.noteRepository = noteRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
  }

  @GetMapping("")
  public UserListingPage listUsers(
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "10") int pageSize)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();

    Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("name").ascending());
    Page<User> userPage = userRepository.findAll(pageable);

    List<UserForListing> users =
        userPage.getContent().stream().map(this::toUserForListing).toList();

    UserListingPage result = new UserListingPage();
    result.setUsers(users);
    result.setPageIndex(pageIndex);
    result.setPageSize(pageSize);
    result.setTotalCount(userPage.getTotalElements());

    return result;
  }

  private UserForListing toUserForListing(User user) {
    UserForListing dto = new UserForListing();
    dto.setId(user.getId());
    dto.setName(user.getName());
    dto.setNoteCount(noteRepository.countByCreator(user.getId()));
    dto.setMemoryTrackerCount(memoryTrackerRepository.countByUser(user.getId()));
    dto.setLastNoteTime(noteRepository.findLastNoteTimeByCreator(user.getId()));
    dto.setLastAssimilationTime(
        memoryTrackerRepository.findLastAssimilationTimeByUser(user.getId()));
    dto.setLastRecallTime(memoryTrackerRepository.findLastRecallTimeByUser(user.getId()));
    return dto;
  }
}
