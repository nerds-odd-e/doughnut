Feature: Showing left panel in the blog

  Background:
    Given I've logged in as an existing user


  Scenario: There is a notebook type blog with title 'odd-e-blog'

    Given There is a blog titled 'odd-e-blog' in Doughnut
    When I add a new blog article with this information
      | Title       | Description       |
      | Why it is so confusing? | Why it is so confusing? This is the content |
    And I open the Blog page
    Then the left panel should show Years list
