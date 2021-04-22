Feature: Publish a note from doughnut to odd-e blog site

  Background:
    Given I've logged in as "developer"

  Scenario: There is a notebook with type Blog with one note in Doughnut
    Given There is a blog titled 'odd-e-blog' in Doughnut
    And There are some notes in the notebook
    | Title       | Description       |
    | Hello World | Content Article 1 |
    When I open the Blog page
    Then I should see a blog post on the Blog page created today
    | Title       | Description       | AuthorName  |
    | Hello World | Content Article 1 | Developer   |

  @ignore
  Scenario: There is no notebook with type Blog in Doughnut
    Given There is no notebook with type Blog in Doughnut
    When I open the Blog page
    Then I should see nothing on the Blog page

  @ignore
  Scenario: There is a notebook with type Blog without any notes in Doughnut
    Given There is a blog titled 'odd-e-blog' in Doughnut
    And   There are no notes in the notebook
    When I open the Blog Page
    Then I should see nothing on the Blog page



  @ignore
  Scenario: There is a notebook with type Blog with multiple notes in Doughnut
    Given There is a blog titled 'odd-e-blog' in Doughnut
    And There are some notes in the notebook
      | title       | description |
      | hello world | hello world |
      | scrum intro | scrum intro |
    When I open the Blog Page
    Then I should see multiple blog posts on the Blog page
      | title       | description |
      | hello world | hello world |
      | scrum intro | scrum intro |

