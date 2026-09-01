Feature: Wiki links after note moves
  As a learner, I want wiki Portable paths to stay correct when notes move
  so inbound and outgoing links keep resolving.

  Background:
    Given I am logged in as an existing user

  @mockBrowserTime
  Scenario: Moving a note across notebooks keeps outgoing wiki links pointed at the old notebook
    Given I have a notebook "WikiMove Old NB" with notes:
      | Title            | Content                                                                | Folder            |
      | WikiMove Target  | old notebook target                                                    | WikiMove Old Root |
      | WikiMove Carrier | Read [[WikiMove Target]] and [[WikiMove Other NB:WikiMove Qualified]]. | WikiMove Old Root |
    And I have a notebook "WikiMove Other NB" with a note "WikiMove Qualified" and content "qualified notebook target"
    And I have a notebook "WikiMove New NB" with a note "WikiMove Target" and content "new notebook target"
    When I route to the note "WikiMove Carrier"
    And I move the current note to notebook "WikiMove New NB" root
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiMove Old NB:WikiMove Target|WikiMove Target]]"
    And the note content markdown source should contain "[[WikiMove Other NB:WikiMove Qualified]]"
    When I view the note content as rich content
    Then the wiki link "WikiMove Target" should open the note titled "WikiMove Target"
    And the note content on the current page should be "old notebook target"

  @mockBrowserTime
  Scenario: Moving a note into a folder across notebooks keeps inbound and outgoing wiki links correct
    Given I have a notebook "WikiFolderMove New NB" with notes:
      | Title                  | Content              | Folder                    |
      | WikiFolderMove Target  | new notebook target  | WikiFolderMove Folder     |
      | WikiFolderMove Carrier | destination namesake | WikiFolderMove Other Root |
    And I have a notebook "WikiFolderMove Old NB" with notes:
      | Title                  | Content                         | Folder                  |
      | WikiFolderMove Target  | old notebook target             | WikiFolderMove Old Root |
      | WikiFolderMove Carrier | Read [[WikiFolderMove Target]]. | WikiFolderMove Old Root |
      | WikiFolderMove Ref     | See [[WikiFolderMove Carrier]]. | WikiFolderMove Old Root |
    When I route to the note "WikiFolderMove Carrier"
    And I move the current note under folder "WikiFolderMove Folder" in notebook "WikiFolderMove New NB"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiFolderMove Old NB:WikiFolderMove Target|WikiFolderMove Target]]"
    When I view the note content as rich content
    Then the wiki link "WikiFolderMove Target" should open the note titled "WikiFolderMove Target"
    And the note content on the current page should be "old notebook target"
    When I route to the note "WikiFolderMove Ref"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiFolderMove New NB:WikiFolderMove Folder/WikiFolderMove Carrier|WikiFolderMove Carrier]]"
    When I view the note content as rich content
    Then the wiki link "WikiFolderMove Carrier" should open the note titled "WikiFolderMove Carrier"

  @mockBrowserTime
  Scenario: Moving a note within a notebook updates exact wiki Portable paths
    Given I have a notebook "WikiLoc Change NB" with notes:
      | Title             | Content        | Folder          |
      | WikiLoc Target    | located target | WikiLoc Recipes |
      | WikiLoc Carrier   | origin         | WikiLoc Root    |
      | WikiLoc Dest note | dest peer      | WikiLoc Pantry  |
    When I update note "WikiLoc Carrier" content using markdown to become:
      """
      See [[WikiLoc Recipes/WikiLoc Target.md|shown]].
      """
    And I route to the note "WikiLoc Target"
    And I move the current note under folder "WikiLoc Pantry" in notebook "WikiLoc Change NB"
    And I route to the note "WikiLoc Carrier"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[WikiLoc Pantry/WikiLoc Target.md|shown]]"
    When I view the note content as rich content
    Then the wiki link "shown" should open the note titled "WikiLoc Target"
    And the note content on the current page should be "located target"
    When I route to the note "WikiLoc Target"
    And I move the current note to notebook "WikiLoc Change NB" root
    And I route to the note "WikiLoc Carrier"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[/WikiLoc Target.md|shown]]"
    When I view the note content as rich content
    Then the wiki link "shown" should open the note titled "WikiLoc Target"
    And the note content on the current page should be "located target"
