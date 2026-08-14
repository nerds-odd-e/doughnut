@mockBrowserTime
@disableOpenAiService
Feature: Accidental match scheduling
  As a learner doing spelling recall
  I want a uniquely matched tracked note brought forward without recall credit
  So that confusion about that note is scheduled sooner without fabricating a recall of it

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Content                        |
      | English  |                                |
      | sedition | Sedition means incite violence |
      | sedation | Put to sleep is sedation       |
    And the notes "English" are skipped from the assimilation sequence
    And It's day 1
    And the note "sedation" was assimilated as spelling on day 1

  Scenario: Unique matched spelling tracker is brought forward without recall credit
    When I visit recall for a due quiz question on day 1
    And I type my answer "sedation"
    Then I should see that my last spelling answer was correct with recall count 1
    And I record the current memory tracker schedule
    And the note "sedition" was assimilated as spelling on day 1
    When I visit recall for a due quiz question on day 1
    Then I should be asked spelling question "means incite violence" from notebook "English practice"
    When I type my answer "sedation"
    Then I should see an accidental match reveal for spelling answer "sedation" with reviewed note "sedition" and matched note "sedation"
    And the spelling memory tracker for "sedation" should be brought forward without recall credit
