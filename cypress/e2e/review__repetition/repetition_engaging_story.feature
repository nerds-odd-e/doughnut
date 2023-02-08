Feature: Review Pages with an Engaging Story
  As a learner, I want to review my old notes through an engaging story, so that I can remember them better.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description     |
      | Sedition | Incite violence |
    And OpenAI thinks that "Sedition" means "To incite violence"

  @ignore
  @usingMockedOpenAiService
  Scenario: I want to review my old notes through an engaging story
    When I request an engaging story on the review page
    Then I should be prompted with a story description "To incite violence"

