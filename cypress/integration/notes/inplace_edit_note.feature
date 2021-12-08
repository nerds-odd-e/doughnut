Feature: In-place Edit
  As a learner, I want to be able to edit note title and description in-place in Article view

  Background:
    Given The user is logged-in
    And The user already has a note with title X and description A

  @ignore
  Scenario: Note title should be updated when edited in-place
    Given The user is already in Article view
    When the title and change it to Y in-place
    Then The title should change to Y

  @ignore
  Scenario: Note description should be updated when edited in-place
    Given The user is already in Article view
    When the description and change it to B in-place
    Then The description should change to B
