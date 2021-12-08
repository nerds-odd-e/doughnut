Feature: In-place Edit
  As a learner, I want to be able to edit note title and description in-place in Article view

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title   | titleIDN  | description      | descriptionIDN   |
      | X       | Indonesia | A                | Bahasa Indonesia |

  @ignore
  Scenario: Note title should be updated when edited in-place
    When I open the "article" view of note "X"
    And I click note title "X"
    When the title and change it to Y in-place
    Then The title should change to Y

  @ignore
  Scenario: Note description should be updated when edited in-place
    Given The user is already in Article view
    When the description and change it to B in-place
    Then The description should change to B
