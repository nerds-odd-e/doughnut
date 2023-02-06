Feature: Suggest description
  As a learner, I want to be given a suggested description for
  my notes.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description         | testingParent |
      | Animals  | Are living beings   |               |
    And that OpenAI thinks that "Animals" means "Sharing the same planet as humans"

  @usingMockedOpenAiService
  Scenario Outline: Perform action with suggested note description
    When I ask for a description suggestion for "Animals"
    Then I should be prompted with a suggested description "Sharing the same planet as humans"
    And I expect that the description will be "<value>" when I "<action>" the suggested description
    Examples:
      | action | value                             |
      | use    | Sharing the same planet as humans |
      | cancel | Are living beings                 |
