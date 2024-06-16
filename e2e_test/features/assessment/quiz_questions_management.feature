Feature: Quiz Question Management
  As a trainer, I want to manage the quiz questions for the notes of a notebook,
  so that I can use the questions for assessment.

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "The cow joke"
    And there are questions for the note:
      | noteTopic    | question             | answer | oneWrongChoice |
      | The cow joke | What does a cow say? | moo    | woo            |

  Scenario: Manually add a question to the note successfully
    When I add the following question for the note "The cow joke":
      | Question                             | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What do you call a cow with not leg? | Ground beef    | Cowboy             | Oxford             |
    Then I should see the questions in the question list of the note "The cow joke":
      | Question                             | Correct Choice |
      | What does a cow say?                 | moo            |
      | What do you call a cow with not leg? | Ground beef    |

  Scenario: Can generate the question by AI
    Given OpenAI now generates this question:
      | Question Stem                            | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | Why do cows have hooves instead of feet? | they lactose   | they moo           | they have          |
    When I generate question by AI for note "The cow joke"
    Then The generated question for the note by AI will show:
      | Stem                                     | Choice 0     | Choice 1 | Choice 2  | Correct Choice Index |
      | Why do cows have hooves instead of feet? | they lactose | they moo | they have | 0                    |

