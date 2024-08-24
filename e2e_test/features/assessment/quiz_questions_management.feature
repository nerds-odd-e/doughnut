Feature: Quiz Question Management
  As a trainer, I want to manage the quiz questions for the notes of a notebook,
  so that I can use the questions for assessment.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "The cow joke"
    And there are questions in the notebook "The cow joke" for the note:
      | Note Topic   | Question             | Answer | One Wrong Choice |
      | The cow joke | What does a cow say? | moo    | woo              |

  Scenario: Manually add a question to the note successfully
    When I add the following question for the note "The cow joke":
      | Stem                                 | Choice 0    | Choice 1 | Choice 2 | Correct Choice Index |
      | What do you call a cow with not leg? | Ground beef | Cowboy   | Oxford   | 0                    |
    Then I should see the questions in the question list of the note "The cow joke":
      | Question                             | Correct Choice |
      | What does a cow say?                 | moo            |
      | What do you call a cow with not leg? | Ground beef    |

  Scenario: Reset approval on new question
    Given I am logged in as an admin
    And I have a notebook with the head note "The cow joke"
    And I request for an approval for notebooks:
      | The cow joke |
    When I add the following question for the note "The cow joke":
      | Stem                                     | Choice 0              | Choice 1 | Choice 2 | Correct Choice Index |
      | Why do cows have hooves instead of feet? | Because they lactose! | Woof!    | What?    | 0                    |
    Then I can request approval for the notebook "The cow joke"

  Scenario: Can delete question
    Given I am logged in as an admin
    And I have a notebook with the head note "The cow joke"
    And I add the following question for the note "The cow joke":
      | Stem                                     | Choice 0              | Choice 1 | Choice 2 | Correct Choice Index |
      | Why do cows have hooves instead of feet? | Because they lactose! | Woof!    | What?    | 0                    |
    And I add the following question for the note "The cow joke":
      | Stem                                 | Choice 0    | Choice 1 | Choice 2 | Correct Choice Index |
      | What do you call a cow with not leg? | Ground beef | Cowboy   | Oxford   | 0                    |
    When I click on the delete button for question "Why do cows have hooves instead of feet?" for note "The cow joke"
    Then I should see the questions in the question list of the note "The cow joke":
      | Question                             | Correct Choice |
      | What do you call a cow with not leg? | Ground beef    |

  @usingMockedOpenAiService
  Scenario: Can generate the question by AI
    Given OpenAI now generates this question:
      | Question Stem                            | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | Why do cows have hooves instead of feet? | they lactose   | they moo           | they have          |
    When I generate question by AI for note "The cow joke"
    Then the question in the form becomes:
      | Stem                                     | Choice 0     | Choice 1 | Choice 2  | Correct Choice Index |
      | Why do cows have hooves instead of feet? | they lactose | they moo | they have | 0                    |

  @usingMockedOpenAiService
  Scenario: Can refine the question by AI
    Given OpenAI now refines the question to become:
      | Question Stem                   | Correct Choice           | Incorrect Choice 1 | Incorrect Choice 2 |
      | Why did the cow cross the road? | To get to the udder side | To see the chicken | To find grass      |
    When I refine the following question for the note "The cow joke":
      | Stem                                 | Choice 1 | Correct Choice Index |
      | What do you call a cow with no legs? | Cowboy   | 0                    |
    Then the question in the form becomes:
      | Stem                            | Choice 0                 | Choice 1           | Choice 2      | Correct Choice Index |
      | Why did the cow cross the road? | To get to the udder side | To see the chicken | To find grass | 0                    |
