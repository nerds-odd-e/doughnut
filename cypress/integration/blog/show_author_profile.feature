Feature: Show author profile

  @ignore
  Scenario: Display author name and description in odd-e blog
    Given an author with the following details had posted a blog
      | Name      | Description    |
      | Author    | I am an author |
    And i visit the blog site
    When i see the blog
    Then i should see author name "Author" and description "I am an author"

  @ignore
  Scenario: Display author avatar and profile picture in odd-e-blog
    Given an author has an avatar and profile picture in doughnut and posted a blog
    And i visit the blog site
    When i see the blog
    Then i should see author avatar and profile picture as in screenshot

  @ignore
  Scenario: Edit author name and description in doughnut and view changes in odd-e blog
    Given I've logged in as an existing user in doughnut
    And I edit my profile as follows
      | Name      | Description    |
      | Author    | I am an author |
    And i visit the blog site
    When i see the blog
    Then i should see my information changes