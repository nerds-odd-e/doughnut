
Feature: Repetition Quiz With Ignored Points
  As a learner, I want to ignore the points in the checklist when generating questions for my note.

  Background: 
    Given I am logged in as an existing user
    And I have a notebook with the head note "Countries" which skips review
    And there are some notes:
      | Title    | Details                        | Skip Memory Tracking | Parent Title |
      | Netherlands | The Netherlands is a country in Europe and also called Holland| false                | Countries      |
 
  @ignore     
  #@usingMockedOpenAiService
  Scenario: AI generated question - ignore checklist topic in question options
    Given I assimilated one note "Netherlands" on day 1
    And one of the checklist topic is selected to ignore "also called Holland"
    When I am recalling my note on day 2
    Then question choices should not include "Holland"

  @ignore
  @usingMockedOpenAiService
  Scenario: AI generated question - ignore checklist topic in question stem
    Given  I assimilated one note "Netherlands" on day 1
    And one of the checklist topic is selected to ignore "also called Holland"
    When I am recalling my note on day 2
    Then question stem should not include "Holland"
