Feature: Delete Understanding Check Points
  As a learner, when I review the understanding checklist,
  I want to delete selected points and have AI remove the related content from note details,
  So that the note becomes more focused on the remaining key points.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" and details "English serves as the world's most widespread lingua franca, vital for global communication, connecting people across cultures and nations. It's the standard language for international business, science, technology, and diplomacy, significantly enhancing career opportunities. English dominates the internet and digital world, with most online content in the language."

  @ignore
  @usingMockedOpenAiService
  Scenario: Successfully delete selected understanding points
    Given OpenAI generates understanding checklist with points:
      | English is the world's most widespread lingua franca |
      | It's vital for global communication and connects people across cultures |
      | English is the standard language for international business, science, and diplomacy |
      | English dominates the internet and digital world |
      | Proficiency in English enhances career opportunities and simplifies travel |
    And OpenAI will delete related content and return new details:
      | English dominates the internet and digital world, with most online content in the language. It's vital for global communication and connects people across cultures. Proficiency in English enhances career opportunities and simplifies travel. |
    And OpenAI will regenerate understanding checklist with points:
      | It's vital for global communication and connects people across cultures |
      | English dominates the internet and digital world |
      | Proficiency in English enhances career opportunities and simplifies travel |
    When I start assimilating "English"
    Then I should see an understanding checklist with a maximum of 5 points
    When I check the understanding point 0
    And I check the understanding point 2
    And I click the delete understanding points button
    And I confirm the deletion
    Then the note details should be "English dominates the internet and digital world, with most online content in the language. It's vital for global communication and connects people across cultures. Proficiency in English enhances career opportunities and simplifies travel."

  @ignore
  @usingMockedOpenAiService
  Scenario: Cancel deleting understanding points
    Given OpenAI generates understanding checklist with points:
      | English is the world's most widespread lingua franca |
      | It's vital for global communication |
      | English is the standard language for business |
      | English dominates the internet |
      | Proficiency in English enhances career opportunities |
    When I start assimilating "English"
    And I check the understanding point 0
    And I check the understanding point 2
    And I click the delete understanding points button
    And I cancel the deletion
    Then the note details should be "English serves as the world's most widespread lingua franca, vital for global communication, connecting people across cultures and nations. It's the standard language for international business, science, technology, and diplomacy, significantly enhancing career opportunities. English dominates the internet and digital world, with most online content in the language."
