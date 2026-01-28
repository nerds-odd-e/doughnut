@ignore
Feature: Promote Point to Sibling Note
  As a learner, when I see understanding points in the assimilation page,
  I want to promote a point to become a sibling note,
  So that I can create a related note at the same level as the current note.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Science" and details "Science is the systematic study of the natural world through observation and experimentation."

  @usingMockedOpenAiService
  Scenario: See two buttons for each understanding point
    Given OpenAI generates understanding checklist with points:
      | Science involves systematic observation |
      | Experimentation is key to scientific method |
    When I start assimilating "Science"
    Then I should see two buttons "Child" and "Sibling" at the end of each point

  @usingMockedOpenAiService
  Scenario: Promote a point to sibling note
    Given OpenAI generates understanding checklist with points:
      | Science involves systematic observation |
      | Experimentation is key to scientific method |
    When I start assimilating "Science"
    And I promote the point "Experimentation is key to scientific method" to a sibling note
    Then a new sibling note "Experimentation is key to scientific method" should be created
    And the point "Experimentation is key to scientific method" should be removed from the understanding checklist
