Feature: read unread comment

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description     |
      | Sedition | Incite violence |

  @ignore
  Scenario: Note with one read comment
    Given I open "Sedition" note from top level
    When I add a comment with description "comment1"
    Then I should see no comments
    And I should see "show comment"

  @ignore @featureToggle
  Scenario: Note with one read and one unread comment
    Given I open "Sedition" note from top level
    And I click the add comment button
    And there is "1" unread comment for note "Sedition"
    Then I should be able to add a comment with description "please elaborate"
    And I should see "show comment"

  @ignore
  Scenario: Note with one unread comment
    Given there is "1" unread comment for note "Sedition"
    And there is "0" read comment for note "Sedition"
    When I open "Sedition" note from top level
    Then I should see 1 comment
    And I should NOT see "show comment"

    When I refresh the page
    Then I should see no comments
    And I should see "show comment"

    When I've logged in as another existing user
    And I open note "Sedition"
    Then I should see 1 comment
    And I should NOT see "show comment"




