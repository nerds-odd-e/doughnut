Feature: Note using wikidata title
    As a learner, I want to be able searching from wikidata resource,
    so that I can create my note using title from wikidata.

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
            | title             | testingParent | description               |
            | Wikidata Notebook |               | Notebook wikidata testing |

    @ignore
    Scenario: Use title to search Wikidata when creating note
        When I create note belonging to "Wikidata Notebook":
            | Title | searchWikidata |
            | Sand  | True           |
        Then I should see search result from wikidata
            | Contain | Length |
            | sand    | 10     |

    @ignore @usingDummyWikidataService
    Scenario: Accept title from wikidata
        When I create note belonging to "Wikidata Notebook":
            | Title | searchWikidata |
            | Sand  | True           |
        And I update the title using wikidata title "first" recommendation
        Then I accept update title confirmation pop up
        And I should see "Sand First" in note title
