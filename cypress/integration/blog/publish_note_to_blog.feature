Feature: Publish a note from doughnut to odd-e blog site

  @ignore
  Scenario: There is no notebook with type Blog in Doughnut
    Given There are no notebook with type blog in Doughnut

    When I open the Blog page
    Then I should see nothing on the blog page

  @ignore
  Scenario: There is a notebook with type blog without any notes
    Given There is a notebook with type blog in Doughnut
    And notebook is titled 'odd-e-blog'
    And   There is no note in the notebook
    When I open the Blog Page
    Then I should see nothing on the blog page

  @ignore
  Scenario: There is a notebook with type blog titled "odd-e-blog" with 1 note
    Given There is a notebook for blog in Doughnut
    And notebook is titled 'odd-e-blog'
    And There are some notes for the current user
      | title  | description               | testingParent |
      | Square | four equal straight sides | Shape         |
    When I open the Blog Page
    Then I should see a blog post titled 'Hello World' on the blog page

  @ignore
  Scenario: There is a notebook for blog with multiple notes
    Given There is a notebook for blog in Doughnut
    And There are some note for the current user
      | title    | description               | testingParent |
      | Shape    | The form of something     |               |
      | Square   | four equal straight sides | Shape         |
      | Triangle | three sides shape         | Shape         |
    When I open the Blog Page
    Then I should see multiple blog post
      | title    | description               | testingParent |
      | Shape    | The form of something     |               |
      | Square   | four equal straight sides | Shape         |
      | Triangle | three sides shape         | Shape         |
