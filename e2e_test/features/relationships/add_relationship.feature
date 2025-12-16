Feature: add relationship
  As a learner, I want to maintain my newly acquired knowledge in
  notes that relate to each other, so that I can review them in the
  future.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Sedition" and details "Incite violence"
    And I have a notebook with the head note "Sedation" and details "Put to sleep"
    And I have a notebook with the head note "Sedative" and details "Sleep medicine"

  @mockBrowserTime
  Scenario: View all linkable notes for a note when no relationship exists
    When I am creating a relationship under note "Sedition"
    Then I should see "Sedation, Sedative" as targets only when searching in all my notebooks " se "
    And I should see note cannot be found when searching in all my notebooks "Sedition"

  @mockBrowserTime
  Scenario Outline: Search note for relationship with partial input
    Given I am creating a relationship under note "Sedition"
    And I should see "<targets>" as targets only when searching in all my notebooks "<search key>"
    Examples:
      | search key | targets            |
      | Sed        | Sedation, Sedative |
      | Sedatio    | Sedation           |

  @mockBrowserTime
  Scenario: creating relationship
    When I add relationship from top level note "Sedition" as "similar to" to note "Sedation"
    And I add relationship from top level note "Sedition" as "similar to" to note "Sedative"
    Then I should see "Sedition" has relationship "similar to" "Sedation, Sedative"

  @mockBrowserTime
  Scenario: Show recently updated notes before search results
    Given I have a notebook with the head note "Recent Note" and details "Recently added"
    When I am creating a relationship under note "Sedition"
    Then I should see "Recent Note" in the recently updated notes section
    When I search for "Sed" in all my notebooks
    Then I should see "Sedation, Sedative" as targets only when searching in all my notebooks "Sed"
    And I should not see the recently updated notes section
