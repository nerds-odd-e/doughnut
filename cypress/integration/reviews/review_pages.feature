Feature: Review Pages
    As a learner, I want to review my notes and links so that I have fresh memory.

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
            | title      | description       |   picture          |
            | Sedition   | Incite violence   |                    |
            | Sedation   | Put to sleep      |                    |
            | Sedative   | Sleep medicine    | a_slide.jpg        |
        And I link note "Sedition" to note "Sedation"
        And I link note "Sedition" to note "Sedative"

    Scenario: Different review pages for different notes
        * Reviews should have review pages in sequence:
            | review type   | title      |   additional info             |
#            | related notes | Sedition   |   Sedation, Sedative          |
            | single note   | Sedition   |   Incite violence             |
            | single note   | Sedation   |   Put to sleep                |
            | picture note  | Sedative   |   Sleep medicine; a_slide.jpg |
            | end of review |            |                               |
