Feature: Create Blog with Year-Date Structure

  Background:
    Given I've logged in as an existing user

  Scenario: After a blog post is created, the blog post's year should appear on blog-site's side navbar
    Given There are no existing blog created in 2021
    And I create a new blog post "How to use Donut" on 20 Apr 2021
    When I visit the blog-site
    Then I should see 2021 on the blog-site's side navbar

  Scenario:  After two blog posts with different years are created, two years should appear on blog-site's side navbar
    Given There is a blog post with title "Post 1" created in 31 Dec 2019
    And There is a blog post with title "Post 2" created in 01 Jan 2020
    When I visit the blog-site
    Then I should see 2019 and 2021 on the blog-site's side navbar
