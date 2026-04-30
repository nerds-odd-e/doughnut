@ignore
Feature: note move
  As a learner, I want to move a note to become a child of another note,   so that I can recall them in the
  future.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Sedition law" with a note "Sedition" and details "Incite violence"
    And I have a notebook "Sedation care" with a note "Sedation" and details "Put to sleep"
    And I have a notebook "Sedative drugs" with a note "Sedative" and details "Sleep medicine"

  @mockBrowserTime
  Scenario: link and move
    When I move note "Sedition" to be under note "Sedation"
    Then I should see "Sedition law/Sedition" with these children
      | note-title   |
      | Sedation     |

  @mockBrowserTime
  Scenario: Undo moving note
    Given I move note "Sedition" to be under note "Sedation"
    When I undo "move note"
    Then I should see "Sedition law/Sedition" with these children
      | note-title   |
      | Sedation     |
