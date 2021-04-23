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

  @ignore
  Scenario: After a blog post is created, the blog post's year should appear on blog-site's side navbar
    Given There are no existing blog created in 2021
    And I create a new blog post "How to use Donut" on 20 Apr 2021
    When I visit the blog-site
    Then I should see 2021 on the blog-site's side navbar

  @ignore
  Scenario:  After two blog posts with different years are created, two years should appear on blog-site's side navbar
    Given There is a blog post with title "Post 1" created in 31 Dec 2019
    And There is a blog post with title "Post 2" created in 01 Jan 2020
    When I visit the blog-site
    Then I should see 2019 and 2021 on the blog-site's side navbar

  @ignore
  Scenario: After creating a blog article, the year note is automatically created
    Given There is a Blog Notebook called odd-e blog
      | Title      | Description |
      | odd-e blog | test        |
    When after creating a blog article "First Article"
      | Title      | Description |
      | First Article | test        |
    Then I should see "odd-e blog, {YYYY}, {MMM}, First Article" in breadcrumb