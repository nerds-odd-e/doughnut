Feature: link note
  As a learner, I want to maintain my newly acquired knowledge in
  notes that linking to each other, so that I can review them in the
  future.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description     |
      | Sedition | Incite violence |
      | Sedation | Put to sleep    |
      | Sedative | Sleep medicine  |

    @stopTime
  Scenario: View all linkable notes for a note when no link exists
    When I am creating link for note "Sedition"
    Then I should see the source note as "Sedition"
    And I should see "Sedation, Sedative" as targets only when searching in all my notes " se "
    And I should see note cannot be found when searching "Sedition"

  @stopTime
  Scenario Outline: Search note for linking with partial input
    Given I am creating link for note "Sedition"
    And I should see "<targets>" as targets only when searching in all my notes "<search key>"
    Examples:
      | search key | targets            |
      | Sed        | Sedation, Sedative |
      | Sedatio    | Sedation           |

  @stopTime
  Scenario: links should show in the view
    When I link note "Sedition" as "similar to" note "Sedation"
    And I link note "Sedition" as "similar to" note "Sedative"
    Then On the current page, I should see "Sedition" has link "similar to" "Sedation, Sedative"
    When I open link "Sedation"
    Then I should be able to change the link to "a specialization of"
    And On the current page, I should see "Sedition" has link "a specialization of" "Sedation"
    When I open link "Sedation"
    Then I should be able to delete the link
    And I should see "Sedition" has no link of type "a specialization of"

  @stopTime
  Scenario: link and move
    Given I link note "Sedition" as "similar to" note "Sedation" and move under it
    When I open "Sedation/Sedition" note from top level
    Then On the current page, I should see "Sedition" has link "similar to" "Sedation"
    When I visit all my notebooks
    Then I should not see note "Sedition" at the top level of all my notes
