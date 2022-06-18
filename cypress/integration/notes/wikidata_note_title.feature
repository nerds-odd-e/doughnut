Feature: Note using wikidata title
    As a learner, I want to be able searching from wikidata resource,
    so that I can create my note using title from wikidata.

    Background:
        Given I've logged in as an existing user
        And I have a note with title "Wikidata Note"
        And I am creating note under "Wikidata Note"

    Scenario: Use title to search Wikidata when creating note
        When I type "sand" in the title and search on Wikidata
        Then I should see 10 search result from wikidata

    @usingDummyWikidataService
    Scenario: Cancel using title from wikidata
        When I type "Rock" in the title and search on Wikidata
        And I select "rock music" with wikidataID "Q11399" from the Wikidata search result
        And I cancel using the note title from wikidata
        Then I should see note title is "Rock"
