Feature: Bazaar browsing
  Part of the bazaar should be visible to everyone.

  Background:
    Given there are some notes for existing user "old_learner"
      | title      | description               | testingParent |
      | Shape      | The form of something     |               |
      | Square     | four equal straight sides | Shape         |
      | Triangle   | three sides shape         | Shape         |
      | Big Square | a square but big          | Square        |
    And note "Shape" is shared to the Bazaar

  Scenario: Browsing as non-user
    When I haven't login
    Then I should see "Shape" is shared in the Bazaar
    And there shouldn't be any note edit button for "Shape"
    When I open the note "Shape" in the Bazaar
    Then there shouldn't be any note edit button for "Square"
    And I should see "Bazaar, Shape" in breadcrumb
    And I should be able to go to the "next" note "Square"

  Scenario: Browsing as non-user in article view
    When I haven't login
    Then I should see "Shape" is shared in the Bazaar
    When I open the note "Shape" in the Bazaar in article view
    Then I should see in the article:
      | level | title      |
      | h1    | Shape      |
      | h2    | Triangle   |
      | h3    | Big Square |

  Scenario: Breadcrumb should be until the share point
