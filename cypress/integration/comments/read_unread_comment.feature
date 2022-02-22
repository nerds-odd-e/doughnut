Feature: read unread comment


  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description     |
      | Sedition | Incite violence |

  @ignore
  Scenario: Note with one read comment
    Given there is "0" unread comment for note "Sedition"
    And there is "1" read comment for note "Sedition"
    When I open note "Sedition"
    Then I should see no comments
    And I should see "show comment" link

  @ignore
  Scenario: Note with one read and one unread comment
    Given there is "1" read comment for note "Sedition"
    And there is "1" unread comment for note "Sedition"
    When I open note "Sedition"
    Then I should see 1 comment
    And I should see "show comment" link

  @ignore
  Scenario: Note with one unread comment
    Given there is "1" unread comment for note "Sedition"
    And there is "0" read comment for note "Sedition"
    When I open note "Sedition"
    Then I should see 1 comment
    And I should NOT see "show comment" link

    When I refresh the page
    Then I should see no comments
    And I should see "show comment" link

    When I've logged in as another existing user
    And I open note "Sedition"
    Then I should see 1 comment
    And I should NOT see "show comment" link




