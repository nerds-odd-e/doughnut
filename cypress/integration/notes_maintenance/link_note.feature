Feature: link note
    As a learner, I want to maintain my newly acquired knowledge in
    notes that linking to each other, so that I can review them in the
    future.

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
            | title           | description            | updatedDatetime          |
            | Sedition        | Incite violence        | 2021-02-19T03:13:45.000Z |
            | Sedation        | Put to sleep           | 2021-02-19T03:13:45.000Z |
            | Sedative        | Sleep medicine         | 2021-02-19T03:13:45.000Z |

    @clean_db
    Scenario: View all my notes
        Then I should see these notes belonging to the user
            | note-title      |
            | Sedition        |
            | Sedation        |
            | Sedative        |

    @clean_db
    Scenario: View all linkable notes when no links exist
        When I am creating link for note "Sedition"
        And I should see the source note as "Sedition"
        And I should see "Sedation, Sedative" as targets only

    @clean_db
    Scenario Outline: Search note for linking with partial input
        Given I am creating link for note "Sedition"
        When I search for notes with title "<search key>"
        And I should see "<targets>" as targets only
      Examples:
        | search key |  targets           |
        | Sed        | Sedation, Sedative |
        | Sedatio    | Sedation           |

    @clean_db
    Scenario: Create link for note
        Given I am creating link for note "Sedition"
        When I link to note "Sedation"
        Then I should see the Sedition note linked to Sedation