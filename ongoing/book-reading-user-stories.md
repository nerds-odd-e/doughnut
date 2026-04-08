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

## Story: Read a range of a book

### Scenarios / sub-stories

- delete a book of a notebook using frontend. It will delete the book record and also remove the file from gcp
- showing the pdf book in book reading page (e2e test needed)
- clicking a book range to jump to pdf position (e2e test needed)
- scroll pdf to update book layout highlight
- show/hide the drawer

---

## Story: Reading record

### Remember book last read position

Given I scroll to a certain position of the book
When I read the book again
Then I should be starting from the same position


### mark a book range as read

When I choose the book range "2.1 Easier to Change—and Harder to Misuse"
And I scroll to title "2.2 Refactoring as Strengthening the Code"
When I mark the book range "2.1 Easier to Change—and Harder to Misuse" as read in the Reading Control Panel
Then I should see that book range "2.1 Easier to Change—and Harder to Misuse" is marked as read in the book layout
And I should see that book range "2.2 Refactoring as Strengthening the Code" is selected in the book layout

### mark a book range with no direct content as read automatically

Given I choose the book range "2. xxx"
When I scroll to title "2.1 xxx" 
Then I should see that book range "2.1 xxxx" is marked as read in the book layout

### mark a book range as skimmed/skipped

---

## Story: Reorganizing the book layout

- use AI to reorg the nesting

---

## Story: Decide or navigate to the next book range to read

_(Sub-stories to be added.)_

---

## Story: Extract a note from a range

_(Sub-stories to be added.)_

---

## Story: Cite the book

_(Sub-stories to be added.)_

---

## Story: EPUB book

_(Sub-stories to be added.)_

## User splits a range further.