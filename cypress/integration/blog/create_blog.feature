Feature: Create Blog with Year-Date Structure

  Background:
    Given I've logged in as an existing user

  Scenario: Create a new blog article in odd-e-blog
    Given There is a blog titled 'odd-e-blog' in Doughnut
    When I add a new blog article in "odd-e-blog" with title "My First Blog Post"
    And I open the Blog page
    Then I should see a blog post on the Blog page created today
      | Title       | Description       | AuthorName  |
      | My First Blog Post |  | Old Learner |

  Scenario: After a blog post is created, the blog post's year should appear on blog-site's side navbar
    Given There is a blog titled 'odd-e-blog' in Doughnut
    When I add a new blog article in "odd-e-blog" with title "My First Blog Post"
    And I open the Blog page
    Then I should see the current year on the blog-site's side navbar