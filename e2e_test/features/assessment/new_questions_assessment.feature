Feature: New questions assessment

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | testingParent | question            | answer |
      | Countries        |               |                     |        |
      | Singapore        | Countries     | Where in the world is Singapore? | Asia   |
      | Vietnam          | Countries     | Most famous food of Vietnam?   | Pho    |

  Scenario: Start an assessment
    Given I start the assessment on "My Notes/Countries" notebook
    When I answer the question "Where in the world is Singapore?" with "Asia"
    When I answer the question "Most famous food of Vietnam?" with "Pho"
    # Then I should see the next question\
