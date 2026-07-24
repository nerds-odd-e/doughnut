@mockBrowserTime
@disableOpenAiService
Feature: Accidental match reveal
  As a learner doing spelling recall
  I want the reviewed note and matched note revealed together when my answer names another note
  So that my confusion becomes visible

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Content                        | Skip Memory Tracking | Remember Spelling |
      | English  |                                | true                 |                   |
      | sedition | Sedition means incite violence |                      | true              |
      | sedation | Put to sleep is sedation       |                      |                   |

  Scenario: Accidental match reveals reviewed and matched notes
    Given It's day 1
    And the note "sedition" was assimilated on day 1
    When I visit recall for a due quiz question on day 2
    Then I should be asked spelling question "means incite violence" from notebook "English practice"
    When I type my answer "sedation"
    Then I should see an accidental match reveal for spelling answer "sedation" with reviewed note "sedition" and matched note "sedation"
