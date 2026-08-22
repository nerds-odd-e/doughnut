Feature: MCQ management
  As a trainer, I want to manage MCQs for notes,
  so that learners can practice with consistent multiple-choice questions.

  Background:
    Given I am logged in as an existing user

  @skipOptimizationDueToKnownNecessarySlowness
  Scenario: Add an MCQ to a note
    Given I have a notebook "Cow jokes" with a note "The cow joke"
    When I add the following question for the note "The cow joke":
      | Stem                                 | Choice 0    | Choice 1 | Choice 2 | Correct Choice Index |
      | What do you call a cow with not leg? | Ground beef | Cowboy   | Oxford   | 0                    |
    Then I should see the questions in the question list of the note "The cow joke":
      | Question                             | Correct Choice |
      | What do you call a cow with not leg? | Ground beef    |

  @usingMockedOpenAiService
  Scenario: Generate then refine a question with AI
    Given I have a notebook "Cow jokes" with a note "The cow joke"
    And OpenAI will return these questions in order:
      | Question Stem                            | Correct Choice           | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | Why do cows have hooves instead of feet? | they lactose             | they moo           | they have          | they graze         |
      | Why did the cow cross the road?          | To get to the udder side | To see the chicken | To find grass      | To reach the barn  |
    When I generate a question with AI for note "The cow joke"
    Then the question in the form becomes:
      | Stem                                     | Choice 0     | Choice 1 | Choice 2  | Correct Choice Index |
      | Why do cows have hooves instead of feet? | they lactose | they moo | they have | 0                    |
    When I refine the question in the form:
      | Stem                                 | Choice 1 | Correct Choice Index |
      | What do you call a cow with no legs? | Cowboy   | 0                    |
    Then the question in the form becomes:
      | Stem                            | Choice 0                 | Choice 1           | Choice 2      | Correct Choice Index |
      | Why did the cow cross the road? | To get to the udder side | To see the chicken | To find grass | 0                    |
