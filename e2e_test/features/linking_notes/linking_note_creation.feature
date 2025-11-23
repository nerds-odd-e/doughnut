Feature: link note
  As a learner, I want to maintain my newly acquired knowledge in
  notes that linking to each other, so that I can review them in the
  future.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Sedition" and details "Incite violence"
    And I have a notebook with the head note "Sedation" and details "Put to sleep"
    And I have a notebook with the head note "Sedative" and details "Sleep medicine"

  @mockBrowserTime
  Scenario: View all linkable notes for a note when no link exists
    When I am creating a linking note under note "Sedition"
    Then I should see "Sedation, Sedative" as targets only when searching in all my notebooks " se "
    And I should see note cannot be found when searching in all my notebooks "Sedition"

  @mockBrowserTime
  Scenario Outline: Search note for linking with partial input
    Given I am creating a linking note under note "Sedition"
    And I should see "<targets>" as targets only when searching in all my notebooks "<search key>"
    Examples:
      | search key | targets            |
      | Sed        | Sedation, Sedative |
      | Sedatio    | Sedation           |

  @mockBrowserTime
  Scenario: creating link
    When I link top level note "Sedition" as "similar to" note "Sedation"
    And I link top level note "Sedition" as "similar to" note "Sedative"
    Then I should see "Sedition" has link "similar to" "Sedation, Sedative"

  @mockBrowserTime
  Scenario: Show recently updated notes before search results
    Given I have a notebook with the head note "Recent Note" and details "Recently added"
    When I am creating a linking note under note "Sedition"
    Then I should see "Recently updated notes" section
    And I should see "Recent Note" in the recently updated notes
    When I search for "Sed" in all my notebooks
    Then I should see "Sedation, Sedative" as targets only when searching in all my notebooks "Sed"
    And I should not see "Recently updated notes" section
