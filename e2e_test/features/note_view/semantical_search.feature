Feature: semantical search
  As a learner, I want to search notes by their semantic meaning,
  so that I can find the notes that I want to view.

  This test use fake embeddings data.
  After the step "OpenAI returns embeddings successfully", the mock service will return the same fake embeddings data for all inputs,
  except for the input "something else".

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Physics" and details "The study of nature"
    And I have a notebook with the head note "Chemistry" and details "The study of substances"

  @mockBrowserTime @usingMockedOpenAiService
  Scenario Outline: Search with semantic search
    Given OpenAI returns embeddings successfully
    And I reindex the notebook "Physics"
    When I start searching from all my notebooks page
    Then I should see "<targets>" as targets only when searching "<search key>"

    Examples:
      | search key     | targets            |
      | matching       | Physics            |
      | chemistry      | Chemistry, Physics |
      | something else |                    |
