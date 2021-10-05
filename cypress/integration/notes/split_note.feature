Feature: split note
  As a learner, I want to split an existing note by each paragraph in each description
  where a paragraph is a group of lines, separated by empty lines.

  Background:
    Given I've logged in as an existing user

  @ignore
  Scenario: Each note has as title the first line
    When I create top level note with:
      | Title    | Description  |
      | PARENT NODE | Note1\nMY NOTE 1\n\nNote2\nMY NOTE 2 |
    When I split the note
    Then we create two child notes with the first lines of each paragraph as title.

  @ignore
  Scenario: Each note has as title the first line and the second line also has description
    When I split the note

  @ignore
  Scenario: Each note has as title the first line and the second line also has description
    Given a note with a description like this:
      | line |.  content |
      | 1.   |           |
      | 2.   |           |
      | 3.   | animal    |
      | 4.   |           |
      | 5.   |           |
    When I split the note
    Then we create only one child note with the first lines of the paragraph as title.

  @ignore
  Scenario: Each note has as title the first line and the second line also has description
    Given a note without description
    When I split the note
    Then we don't split it
