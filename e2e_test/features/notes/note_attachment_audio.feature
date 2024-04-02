Feature: Attach audio file
  As a learner, I want to attach audio file to my notes so that I can review them in the future.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | testingParent  | details             |
      | FourK            |                | I'm testing.        |

  Scenario: Attach the new audio file successful 
    When I attach audio file "spring.mp3" to my note
    Then I should see "spring.mp3" in my note

  Scenario: Override attach the audio file successful 
    Given My note already has "spring.mp3"
    When I attach audio file "autumn.mp3" to my note
    Then I should see "autumn.mp3" in my note