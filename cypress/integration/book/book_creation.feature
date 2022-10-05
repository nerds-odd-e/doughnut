Feature: Book note with nested author
  As a user, I want to create a note from a book by the title
  and optionally add and/or link an author note

  Background:
    Given I've logged in as an existing user
    And I create a note

  Scenario: A note is created for an existing book with known author.
    Given there is a book "Rage" authored by "Stephen King" with WikiData ID "Q277260"
    When I create the note "Rage" associated with WikiData ID "Q277260"
    Then I expect a note "Rage" with a note "Stephen King" as the author of the book "Rage"

  Scenario: A note is created for an existing book with unknown author.
    Given there is a book "Beowulf" unauthored with WikiData ID "Q48328"
    When I create the note "Beowulf" associated with WikiData ID "Q48328"
    Then I expect a note "Beowulf" with no linked note to an author.
    
