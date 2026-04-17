# Book reading user stories

## Story: Add a PDF book to a notebook and browse its structure

### Scenario: Attach PDF and see structure in the browser

```gherkin
Given there is a notebook "Top Maths"
When I attach book "top-maths.pdf" to the notebook "Top Maths" via the CLI
Then I should see the book layout of the notebook "Top Maths" in the browser
```

### Sub-stories

- User can `/use <notebook>` to select the current active notebook in the CLI.
- User can `/attach <pdf file>` so the book is parsed and shared with the Doughnut server.
- User can see the book layout of a notebook.

---

## Story: Read a block of a book

### Scenarios / sub-stories

- delete a book of a notebook using frontend. It will delete the book record and also remove the file from gcp
- showing the pdf book in book reading page (e2e test needed)
- clicking a book block to jump to pdf position (e2e test needed)
- scroll pdf to update book layout highlight
- show/hide the drawer

---

## Story: Reading record

### Remember book last read position

Given I scroll to a certain position of the book
When I read the book again
Then I should be starting from the same position


### mark a book block as read

When I choose the book block "2.1 Easier to Change—and Harder to Misuse"
And I scroll the PDF until the book block "2.2 Refactoring as Strengthening the Code" is the current block in the book reader
When I mark the book block "2.1 Easier to Change—and Harder to Misuse" as read in the Reading Control Panel
Then I should see that book block "2.1 Easier to Change—and Harder to Misuse" is marked as read in the book layout
And I should see that book block "2.2 Refactoring as Strengthening the Code" is selected in the book layout

### mark a book block with no direct content as read automatically

Given I choose the book block "2. xxx"
When I scroll the PDF until the book block "2.1 xxx" is the current block in the book reader 
Then I should see that book block "2.1 xxxx" is marked as read in the book layout

### mark a book block as skimmed/skipped

When I choose the book block "2.1 Easier to Change—and Harder to Misuse"
And I scroll the PDF until the book block "2.2 Refactoring as Strengthening the Code" is the current block in the book reader
When I mark the book block "2.1 Easier to Change—and Harder to Misuse" as skimmed in the Reading Control Panel
Then I should see that book block "2.1 Easier to Change—and Harder to Misuse" is marked as skimmed in the book layout
And I should see that book block "2.2 Refactoring as Strengthening the Code" is selected in the book layout

When I choose the book block "2.1 Easier to Change—and Harder to Misuse"
And I scroll the PDF until the book block "2.2 Refactoring as Strengthening the Code" is the current block in the book reader
When I mark the book block "2.1 Easier to Change—and Harder to Misuse" as skipped in the Reading Control Panel
Then I should see that book block "2.1 Easier to Change—and Harder to Misuse" is marked as skipped in the book layout
And I should see that book block "2.2 Refactoring as Strengthening the Code" is selected in the book layout

---

## Story: Reorganizing the book layout

- improve the UX/UI of the book layout list, to make the nesting more clear
- drag layout item right to increase depth
- drag item left to decrease depth
- dragging an item followed by cildrens should do indentation together
- tab / shift-tab keys to increase and decrease
- use AI to reorg the depth
- cancel a book block and merge the content to the previous book block
- create a book block
- create a book block that the selected content block is too long or has no text as book block title


---

## Story: Decide or navigate to the next book block to read

- space key to move to next block or viewport

---

## Story: Extract a note from a book block

_(Sub-stories to be added.)_

---

## Story: Cite the book

_(Sub-stories to be added.)_

---

## Story: EPUB book

### Attach an EPUB from the CLI (shipped)

Given I have selected a notebook in the CLI and an `.epub` file on disk
When I run `/attach <file.epub>`
Then the EPUB is uploaded as-is (no MinerU, no client preprocessing)
And the notebook shows the attached EPUB with its chapter structure

### Mark an EPUB block as read

Given an EPUB is open in the reader
When I click a block in the book layout
And I mark that block as read in the Reading Control Panel
Then I should see that block marked as read in the book layout
And I should see the next block selected in the book layout

### Skim or skip an EPUB block (shipped)

Given an EPUB is open in the reader and a block with direct content is selected
When I mark that block as skimmed (or skipped) in the Reading Control Panel
Then I should see that block marked as skimmed (or skipped) in the book layout
And I should see the next block selected in the book layout

### Auto-mark a structural-only EPUB block on entering its successor (shipped)

Given an EPUB block has no direct content and its successor does
When I scroll (or click) into the successor block
Then the structural-only predecessor is marked as read in the book layout

## User splits a book block further.