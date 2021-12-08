Feature: detect note update conflicts
  As a user, when I am editing some note together with other users,
  I want to detect edit conflicts.

  Background:
    Given There is a circle "Odd-e Team" with "old_learner, another_old_learner" members
    And I've logged in as "old_learner"
    And I create a note "Shared Note" in circle "Odd-e Team"

  @ignore
  Scenario: I should see conflict message when updating title
    When I am editing note "Shared Note" the field should be pre-filled with
      | Title          | Description |
      | Shared Note    |             |
    And Another user updates note "Shared Note" with:
      | Title           | Description |
      | The Shared Note |             |   
    And I update it to become:
      | Title          | Description |
      | My Shared Note |             |
    Then I should see "Conflict detected!" message
