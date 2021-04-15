Feature: Frontend Bazaar view
  Frontend view of public notes (i.e. Bazaar Notes only)

        Background:
            Given there are some notes for existing user "old_learner"
                  | title           | description                      | testingParent | hideTitleInArticle | showAsBulletInArticle |
                  | Shape           | The form of something            |               | false              | false                 |
                  | Rectangle       | four equal straight sides        | Shape         | false              | false                 |
                  | Square          | a square but big                 | Rectangle     | false              | false                 |
                  | Shapes are good |                                  | Shape         | false              | false                 |
              And there is "belongs to" link between note "Square" and "Rectangle"
              And note "Shape" is shared to the Bazaar
        @focus
        Scenario: Browsing as non-user
             When I visit frontend app
             Then I should see the frontend app screenshot matches
