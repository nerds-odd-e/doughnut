Feature: Publish a note from doughnut to odd-e blog site

  Scenario: There is a notebook with type Blog with one note in Doughnut
    Given There is a notebook titled 'odd-e-blog' with type Blog in Doughnut
    And There are some notes in the notebook
    When I open the blog page
    Then I should see a blog post titled 'Hello World' on the Blog page

  @ignore
  Scenario: There is no notebook with type Blog in Doughnut
    Given There is no notebook with type Blog in Doughnut
    When I open the Blog page
    Then I should see nothing on the Blog page

  @ignore
  Scenario: There is a notebook with type Blog without any notes in Doughnut
    Given There is a notebook titled 'odd-e-blog' with type Blog in Doughnut
    And   There are no notes in the notebook
    When I open the Blog Page
    Then I should see nothing on the Blog page



  @ignore
  Scenario: There is a notebook with type Blog with multiple notes in Doughnut
    Given There is a notebook titled 'odd-e-blog' with type Blog in Doughnut
    And There are some notes in the notebook
      | title       | description |
      | hello world | hello world |
      | scrum intro | scrum intro |
    When I open the Blog Page
    Then I should see multiple blog posts on the Blog page
      | title       | description |
      | hello world | hello world |
      | scrum intro | scrum intro |

