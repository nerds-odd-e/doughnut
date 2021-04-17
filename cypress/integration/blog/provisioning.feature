Feature: Provisioning Blog from Note

  Background:
    Given I've logged in as an existing user


  Scenario: provisioning Blog from Note
    Given There is a Blog Notebook called odd-e blog
      | Title      | Description |
      | odd-e blog | test        |
    And  A blog is posted in "odd-e blog"
      | Title           | Description |
      | how to do Scrum | Scrum       |
    When Use odd-e blog api from a third party app
    Then You can see a blog "how to do Scrum" from a third party app
