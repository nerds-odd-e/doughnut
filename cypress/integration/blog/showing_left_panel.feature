Feature: Showing left panel in the blog


  @ignore
  Scenario: There is no any notebook type blog

    Given There is no Notebook type blog
    When I open the blog page
    Then the left panel should show empty


  @ignore
  Scenario: There is a notebook type blog with title 'odd-e-blog'

    Given There is a Notebook type blog with title 'odd-e-blog'
    When I open the blog page
    Then the left panel should show Years list

  @ignore
  Scenario: There is a notebook type blog with title 'odd-e-blog' without a year note linked

    Given There is a Notebook type blog with title 'odd-e-blog' without a year note linked to it
    When I open the blog page
    Then the left panel should show empty

