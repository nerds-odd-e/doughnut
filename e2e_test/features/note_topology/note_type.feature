Feature: Note type
  As a learner, I want to have note type for my note. It will be used to generate questions for my note.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Reservoirs" and details "The most popular reservoir to hike in is Macritchie"
    And AI will generate question for note with type:
     | note type | question                                       |
     | experience | What is the most popular reservoir to hike in? |
     | source     | What does the word reservoir mean?             |

  @usingMockedOpenAiService
  Scenario Outline: AI will interpret note type when generating questions
    Given I assimilated one note "Reservoirs" on day 1
    And I assign note type "<Note Type>" for note "Reservoirs"
    When I am recalling my note on day 2
    Then I should be asked "<Question>"

    Examples:
    | Note Type | Question                                       |
    | experience | What is the most popular reservoir to hike in? |
    | source     | What does the word reservoir mean?             |

    