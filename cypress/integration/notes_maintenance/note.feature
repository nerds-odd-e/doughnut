Feature: Note maintenance
    As a learner, I want to maintain my newly acquired knowledge in
    notes that linking to each other, so that I can review them in the
    future.

@clean_db @login_as_existing_user1
Scenario: New user create notes after login
    When I create note with: 
    | note-title      |   note-description  |
    | Sedation        |   Put to sleep      |
    Then I should see the note with title and description on the review page
    | note-title      |   note-description  |
    | Sedation        |   Put to sleep      |

@clean_db @login_as_existing_user1
Scenario: New user review multiple notes
    Given I have some notes
    | note-title      |   note-description  | note-updatedDateTime     |
    | Sedition        |   Incite violence   | 2021-02-19T02:42:07.597Z |
    | Sedation        |   Put to sleep      | 2021-02-19T02:42:08.597Z |
    When I review my notes
    Then I should see the note with title and description on the review page
    | note-title      |   note-description  |
    | Sedation        |   Put to sleep      |
    And I click on next note
    Then I should see the note with title and description on the review page
    | note-title      |   note-description  |
    | Sedition        |   Incite violence   |

@clean_db @login_as_existing_user1
Scenario: : Get a list of notes
    Given I have some notes
    | note-title      |   note-description  | note-updatedDateTime     |
    | A               |   This ia a A       | 2021-02-19T02:42:07.597Z |
    | B               |   This is a B       | 2021-02-19T02:42:08.597Z |
    When I review my notes
    Then I should see the note with title and description on the review page
    | note-title      |   note-description  |
    | B               |   This is a B      |

    
@clean_db @login_as_existing_user1
Scenario: New user review note with link
    Given I link Sedition to Sedation
    | note-title      |   note-description  |
    | Sedition        |   Incite violence   |
    | Sedation        |   Put to sleep      |
    When I review my notes
    Then I should see following note with links on the review page
    | note-title      |   note-links |
    | Sedition        |   Sedation   |
    And I click on next note
    Then I should see the note with title and description on the review page
    | note-title      |   note-description  |
    | Sedition        |   Incite violence   |
    And I click on next note
    Then I should see the note with title and description on the review page
    | note-title      |   note-description  |
    | Sedation        |   Put to sleep      |

