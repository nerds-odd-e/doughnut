Feature: split note
  As a learner, I want to split an existing note by each paragraph in each description
  where a paragraph is a group of lines, separated by empty lines.

  Background:
    Given I've logged in as an existing user

  @ignore
  Scenario: Each note has as title the first line and the second line also has description
    Given there are some notes for the current user
      | title                     | description                          |
      | Split note LeSS in Action | Note1\nMY NOTE 1\n\nNote2\nMY NOTE 2 |
    When I split note "Split note LeSS in Action"
    Then The second line of the description is in the note description

  @ignore
  Scenario: Each note has as title the first line and the second line also has description
    Given a note with a description like this:
      | line | .  content |
      | 1.   |            |
      | 2.   |            |
      | 3.   | animal     |
      | 4.   |            |
      | 5.   |            |
    When I split the note
    Then we create only one child note with the first lines of the paragraph as title.

  @ignore
  Scenario: Each note has as title the first line and the second line also has description
    Given a note without description
    When I split the note
    Then we don't split it

  @featureToggle
  Scenario: Each note has as title the first line
    Given there are some notes for the current user
      | title   | description                                     |
      | animals | canine\nDogs, wolves, etc\n\nHomo\nWe are homos |
    When I split note "animals"
    Then I should see the note description to be ""
    And there is a note "animals/canine" with description "Dogs, wolves, etc"
    And there is a note "animals/Homo" with description "We are homos"
