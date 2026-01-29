@ignore
Feature: Ignore Understanding Check Points When Recalling
  As a learner, when I mark selected understanding points as ignored for recall,
  I want those points' questions not to appear when I recall.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" and details "English serves as the world's most widespread lingua franca, vital for global communication, connecting people across cultures and nations. It's the standard language for international business, science, technology, and diplomacy, significantly enhancing career opportunities. English dominates the internet and digital world, with most online content in the language."

  @usingMockedOpenAiService
  Scenario: Ignored points' questions do not appear on recall page
    Given OpenAI generates understanding checklist with points:
      | English is the world's most widespread lingua franca |
      | It's vital for global communication and connects people across cultures |
      | English is the standard language for international business, science, and diplomacy |
    And OpenAI generates this question:
      | Question Stem                          | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is key about global communication? | connects people   | business           | diplomacy          |
    And It's day 1
    When I start assimilating "English"
    And I ignore these understanding points and complete assimilation:
      | English is the world's most widespread lingua franca |
      | English is the standard language for international business, science, and diplomacy |
    When I am recalling my note on day 2
    Then I should be asked "What is key about global communication?"
