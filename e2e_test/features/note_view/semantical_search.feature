Feature: semantical search
  As a learner, I want to search notes by their semantic meaning,
  so that I can find the notes that I want to review.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "the Universe" and notes:
      | Title            | Parent Title   |
      | Topic1           | the Universe   |
      | Topic2           | the Universe   |

  @mockBrowserTime
  @enableSemanticSearch
  Scenario Outline: Search at the top level
    Given I update note "Topic1" to become:
      | Title     | Details           |
      | Galaxy    | also called "Milky Way" |
    And I update note "Topic2" to become:
      | Title     | Details           |
      | Earth     | also called "the blue planet" |
    When I start searching
    Then I should see "<targets>" as targets only when searching "<search key>"
    Examples:
      | search key | targets     |
      | Galaxy     | Galaxy      |
      | Earth      | Earth       |
