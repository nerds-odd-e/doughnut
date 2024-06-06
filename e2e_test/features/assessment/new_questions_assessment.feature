Feature: New questions assessment

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | testingParent |
      | Countries        |               |
      | Singapore        | Countries     |
      | Vietnam          | Countries     |
    And notebook "Countries" is shared to the Bazaar
    And there are questions for the note:
      | topicConstructor | question                         | answer | option |
      | Singapore        | Where in the world is Singapore? | Asia   | euro   |
      | Vietnam          | Most famous food of Vietnam?     | Pho    | bread  |
    When I go to the bazaar

  Scenario: Start an assessment
    When I start the assessment on the "Countries" notebook
    Then I answer the question "Where in the world is Singapore?" with "Asia"
    And I answer the question "Most famous food of Vietnam?" with "Pho"
