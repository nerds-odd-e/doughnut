Feature: Note type
  As a learner, I want to add note type to my note

  Background:
    Given I am logged in as an existing user

  Scenario: Adding note type to my new note
    Given I have a notebook with the head note "Reservoirs" and details "The most popular reservoir to hike in is Macritchie"
    When I navigate to "Reservoirs" note
    And I add note type "concept" to my note
    Then I will see new type "concept" on my note
    
  Scenario: Changing note type of existing note
    Given I have a notebook with the head note "Reservoirs" and details "The most popular reservoir to hike in is Macritchie"
    When I navigate to "Reservoirs" note
    And I add note type "concept" to my note
    And I add note type "journal" to my note
    Then I will see new type "journal" on my note

  @usingMockedOpenAiService
  Scenario Outline: AI will interpret note type when generating questions
    Given I have a notebook with the head note "Reservoirs" and details "The most popular reservoir to hike in is Macritchie"
    And AI will generate question for note with type:
     | note type | question                                       |
     | journal   | What is the most popular reservoir to hike in? |
     | vocab     | What does the word reservoir mean?            |
    And I learned one note "Reservoirs" on day 1
    When I assign note type "<Note Type>" for note "Reservoirs"
    And I am recalling my note on day 2
    Then AI will generate question for note "Reservoirs" with question "<Question>"

    Examples:
    | Note Type | Question                                       |
    | journal   | What is the most popular reservoir to hike in? |
    | vocab     | What does the word reservoir mean?             |

    