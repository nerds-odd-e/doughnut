@ignore
Feature: Convert SRT format to Text
  As a learner, I want to convert the SRT format to text in the Note details.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | details             |
      | SRT format   | 1
                       00:05:00,400 --> 00:05:15,300
                       This is an example of
                       a subtitle. |

  Scenario: Convert srt format to text
    Given I have note with SRT format
    When I convert it to text format
    Then I get note with text "This is an example of a subtitle."


