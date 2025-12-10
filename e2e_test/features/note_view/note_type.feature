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
    