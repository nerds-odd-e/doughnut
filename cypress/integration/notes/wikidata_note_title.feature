Feature: Note using wikidata title
    As a learner, I want to maintain my newly acquired knowledge in
    notes, so that I can review them in the future.

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
            | title             | testingParent | description               |
            | Wikidata Notebook |               | Notebook wikidata testing |

    @ignore
    Scenario: Use title to search Wikidata when creating note
        When I create note belonging to "Wikidata Notebook":
            | Title | searchWikidata |
            | Snake | True           |
        Then I should see search result from wikidata
            | Contain | Length |
            | snake   | 10     |
