
Feature: Notebook questions

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "LeSS in Action" and notes:
      | Topic | Parent Topic   |
      | tech  | LeSS in Action |
      | management  | LeSS in Action |
      | team  | management           |

  Scenario: I have a notebook with no questions
    Then I should see that there are no questions for "LeSS in Action" for the following topics:
      | Topic |
      | management  |

  Scenario: View all questions in a notebook
    When I add questions to the following notes in the notebook "LeSS in Action"
      | Topic | Question                |
      | team  | Who is the team?        |
      | tech  | What is the technology? |
    Then I should see the following questions for the topics in the notebook "LeSS in Action":
      | Topic | Question                |
      | team  | Who is the team?        |
      | tech  | What is the technology? |
    And I should see that there are no questions for "LeSS in Action" for the following topics:
      | Topic |
      | management  |

  Scenario: Add question to child note
    When I add the following question for the note "team" of notebook "LeSS in Action":
      | Stem             | Choice 0 | Choice 1 | Choice 2 | Correct Choice Index |
      | Who is the team? | Us       | Them     | No one   | 0                    |
    Then I should see the following questions for the topics in the notebook "LeSS in Action":
      | Topic | Question         |
      | team  | Who is the team? |
    And I should see that there are no questions for "LeSS in Action" for the following topics:
      | Topic      |
      | tech       |
      | management |
