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

- use AI to reorg the nesting

---

## Story: Decide or navigate to the next book block to read

_(Sub-stories to be added.)_

---

## Story: Extract a note from a book block

_(Sub-stories to be added.)_

---

## Story: Cite the book

_(Sub-stories to be added.)_

---

## Story: EPUB book

_(Sub-stories to be added.)_

## User splits a book block further.