@ignore
@usingMockedOpenAiService
Feature: Refine note details

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic | details |
      | TDD | TDD a development process rellying on software requirements being converfted to test cases before software fuly developed. |

  Scenario:Refine note details and approve
    Given I have a note with the topic "TDD"
    When I click the Refine button
    Then I see the refined note details from AI as "Test-driven development (TDD) is a software development process relying on software requirements being converted to test cases before software is fully developed."
    And I approve the refined note details
    Then I see the note datails text becomes "Test-driven development (TDD) is a software development process relying on software requirements being converted to test cases before software is fully developed."

  Scenario:Refine note details and decline
    Given I have opened the note with topic TDD
    When I click the Refine button
    Then I see the refined note details from AI as "Test-driven development (TDD) is a software development process relying on software requirements being converted to test cases before software is fully developed."
    And I decline the refined note details
    Then I see the note datails text stays "TDD a development process rellying on software requirements being converfted to test cases before software fuly developed."
