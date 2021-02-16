Feature: Note related feature

@clean_db
Scenario: New user create a note
    When I did not log in
    And I create note
    Then I should be asked to log in
