Feature: Semantic note search
  As a learner, I want to search notes by their semantic meaning,
  so that I can find the notes that I want to view.

  This test use fake embeddings data.
  After the step "OpenAI returns embeddings successfully", the mock service will return the same fake embeddings data for all inputs,
  except for the input "something else".

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Physics primer" with a note "Physics" and content "The study of nature"
    And I have a notebook "Chemistry primer" with a note "Chemistry" and content "The study of substances"
    And OpenAI returns embeddings successfully
    And I reindex the notebook "Physics primer"

  @mockBrowserTime @usingMockedOpenAiService
  Scenario Outline: Semantic search finds notes by meaning
    When I start searching notes
    And I enable semantic search
    Then I should see "<targets>" as targets only when searching "<search key>"

    Examples:
      | search key     | targets            |
      | matching       | Physics            |
      | chemistry      | Chemistry, Physics |
      | something else |                    |

  @mockBrowserTime @usingMockedOpenAiService
  Scenario: Semantic search results show which notebook each note is from
    When I start searching notes
    And I enable semantic search
    Then I should see "Physics" as targets only when searching "matching"
    And I should see notebook "Physics primer" in search results
