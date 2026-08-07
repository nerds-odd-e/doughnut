Feature: Reorganize book layout

  Background:
    Given I am logged in as an existing user

  Rule: Change depth with keyboard

    Background:
      Given I have a notebook "Refactoring read" with a note "Code Refactoring Book"
      And I attach a fake blank pdf book with layout of "refactoring" to the notebook "Code Refactoring Book"
      And I open the book attached to notebook "Refactoring read"

    Scenario Outline: Indent a book block with Tab
      Given the book layout shows block "<block>" at depth <start_depth>
      When I choose the book block "<block>"
      Then the book block "<block>" should be focused in the book layout
      When I indent the focused book block with Tab
      Then the book block "<block>" should be at depth <end_depth> in the book layout

      Examples:
        | block                                   | start_depth | end_depth |
        | 2. The Usual Defi nition Is Not Enough  | 0           | 1         |

    Scenario Outline: Outdent a book block with Shift+Tab
      Given the book layout shows block "<block>" at depth <start_depth>
      When I choose the book block "<block>"
      Then the book block "<block>" should be focused in the book layout
      When I outdent the focused book block with Shift+Tab
      Then the book block "<block>" should be at depth <end_depth> in the book layout

      Examples:
        | block                               | start_depth | end_depth |
        | 3.1 Can You Refactor Without Tests? | 1           | 0         |

  Rule: Content block bbox overlays

    Background:
      Given I have a notebook "Refactoring read" with a note "Code Refactoring Book"
      And I attach a fake blank pdf book with layout of "refactoring" to the notebook "Code Refactoring Book"
      And I open the book attached to notebook "Refactoring read"

    @mockBrowserTime
    Scenario: Content block bboxes are visible while a block is selected
      When I choose the book block "1. Refactoring: Protecting Intention in Working Software"
      Then I should see content block bbox overlays on the PDF

    @mockBrowserTime
    Scenario: Create a new book block from a content block bbox
      When I choose the book block "1. Refactoring: Protecting Intention in Working Software"
      And I create a book block from a content block on the PDF
      Then I should see the "New block" callout
      When I confirm creating a new block
      Then the book layout should contain a new block as a child of the selected block

    @mockBrowserTime
    Scenario: Create a book block from long content bbox with a typed title
      When I choose the book block "1. Refactoring: Protecting Intention in Working Software"
      And I create a book block from a long-text content block on the PDF
      Then I should see the "New block" callout
      When I confirm creating a new block
      Then I should be prompted to enter a title defaulting to truncated content
      When I confirm the title
      Then the book layout should contain a new block as a child of the selected block

  Rule: Change depth of a block with its descendants or cancel a block

    Background:
      Given I have a notebook "Subtree read" with a note "Subtree Book"
      And I attach a fake blank pdf book with layout of "subtree_indent" to the notebook "Subtree Book"
      And I open the book attached to notebook "Subtree read"

    Scenario: Indent a block and its children together
      Given the book layout shows block "Chapter A" at depth 0
      When I choose the book block "Chapter A"
      Then the book block "Chapter A" should be focused in the book layout
      When I indent the focused book block with Tab
      Then the book block "Chapter A" should be at depth 1 in the book layout
      And the book block "A.1 First section" should be at depth 2 in the book layout
      And the book block "A.2 Second section" should be at depth 2 in the book layout
      And the book block "Chapter B" should be at depth 0 in the book layout

    Scenario: Cancel a leaf block removes it from the layout
      When I choose the book block "Chapter B"
      Then the book block "Chapter B" should be focused in the book layout
      When I cancel the focused book block with Backspace
      Then the book block "Chapter B" should no longer appear in the book layout

    Scenario: Cancel a parent block promotes its children
      When I choose the book block "Chapter A"
      Then the book block "Chapter A" should be focused in the book layout
      When I cancel the focused book block with Backspace
      Then the book block "Chapter A" should no longer appear in the book layout
      And the book block "A.1 First section" should be at depth 0 in the book layout
      And the book block "A.2 Second section" should be at depth 0 in the book layout
