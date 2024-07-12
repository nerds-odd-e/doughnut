Feature: Assessment History
  As a trainee I want to see my assessment history,
  so that I can track my progress
  and review/share my results with others

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Countries"
    And notebook "Countries" is shared to the Bazaar
    And there are questions for the note:
      | Note Topic | Question                         | Answer | One Wrong Choice | Approved |
      | Countries  | Where in the world is Singapore? | Asia   | Who knows?       | true     |
    And I set the number of questions per assessment of the notebook "Countries" to 1

  Scenario: "View assessment history"
    When I submit the assessment on the "Countries" notebook in the bazaar
    Then I must see the following assessments in my assessment history:
      | notebook topic | score | total questions |
      | Countries      | 0     | 1               |

