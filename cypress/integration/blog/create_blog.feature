Feature: Create Blog with Year-Date Structure

  Background:
    Given I've logged in as an existing user

  Scenario: Create a new blog article in odd-e-blog
    Given There is a blog titled 'odd-e-blog' in Doughnut
    When I add a new blog post in "odd-e-blog" on "2020/04/21" with title "My First Blog Post"
