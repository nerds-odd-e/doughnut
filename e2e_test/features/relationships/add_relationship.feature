Feature: Add relationship
  As a learner, I want to maintain my newly acquired knowledge in
  notes that relate to each other, so that I can recall them in the
  future.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Sedition law" with a note "Sedition" and content "Incite violence"
    And I have a notebook "Sedation care" with a note "Sedation" and content "Put to sleep"
    And I have a notebook "Sedative drugs" with a note "Sedative" and content "Sleep medicine"

  @mockBrowserTime
  Scenario: Searching for relationship targets when none exist yet
    When I open wiki link or relationship for note "Sedition"
    Then I should see "Sedation, Sedative" as targets only when searching in all notebooks " se "
    And I should see note cannot be found when searching in all notebooks "Sedition"

  @mockBrowserTime
  Scenario Outline: Searching for relationship targets with partial input
    When I open wiki link or relationship for note "Sedition"
    Then I should see "<targets>" as targets only when searching in all notebooks "<search key>"
    Examples:
      | search key | targets            |
      | Sed        | Sedation, Sedative |
      | Sedatio    | Sedation           |

  @mockBrowserTime
  Scenario: Creating relationships between notes
    When I add a relationship from note "Sedition" as "similar to" to note "Sedation"
    And I add a relationship from note "Sedition" as "similar to" to note "Sedative"
    Then I should see "Sedition" has relationship "similar to" "Sedation, Sedative"
    And I should see "Sedative" has relationship "similar to" "Sedition, Sedative"

  @mockBrowserTime
  Scenario: Recently updated notes appear before search results
    Given I have a notebook "Recent scratch" with a note "Recent Note" and content "Recently added"
    When I open wiki link or relationship for note "Sedition"
    Then I should see "Recent Note" in the recently updated notes section
    When I search for "Sed" in all notebooks
    Then I should see "Sedation, Sedative" as targets only when searching in all notebooks "Sed"
    And I should not see the recently updated notes section

  @mockBrowserTime
  Scenario: Switching to recently updated notes while search key is non-empty
    Given I have a notebook "Recent scratch" with a note "Recent Note" and content "Recently added"
    When I open wiki link or relationship for note "Sedition"
    And I search for "Sed" in all notebooks
    Then I should see relationship targets "Sedation, Sedative"
    And I should not see the recently updated notes section
    When I switch to recently updated notes
    Then I should see "Recent Note" in the recently updated notes section
    And the note search field should contain "Sed"
    When I switch to matching notes
    Then I should see relationship targets "Sedation, Sedative"

  @mockBrowserTime
  Scenario: Undoing relationship creation
    When I add a relationship from note "Sedition" as "similar to" to note "Sedation"
    Then I should see "Sedition" has relationship "similar to" "Sedation"
    When I open the relationship from "Sedition" to "Sedation"
    Then I should be on the relationship note page from "Sedition" with relation "similar to" to "Sedation"
    When I open the note content markdown editor
    Then the note content markdown source should contain "type: Relationship"
    And the note content markdown source should contain "relation: similar-to"
    And the note content markdown source should contain 'source: "[[Sedition]]"'
    And the note content markdown source should contain 'target: "[[Sedation care: Sedation]]"'
    When I undo "create note"
    Then I should see "Sedition" has no relationship to "Sedation"
