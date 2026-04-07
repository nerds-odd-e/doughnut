# Book reading user stories

## Story: Add a PDF book to a notebook and browse its structure

### Scenario: Attach PDF and see structure in the browser

```gherkin
Given there is a notebook "Top Maths"
When I attach book "top-maths.pdf" to the notebook "Top Maths" via the CLI
Then I should see the book structure of the notebook "Top Maths" in the browser
```

### Sub-stories

- User can `/use <notebook>` to select the current active notebook in the CLI.
- User can `/attach <pdf file>` so the book is parsed and shared with the Doughnut server.
- User can see the book structure of a notebook.

---

## Story: Read a range of a book

### Scenarios / sub-stories

- delete a book of a notebook using frontend. It will delete the book record and also remove the file from gcp
- showing the pdf book in book reading page (e2e test needed)
- clicking book layout range to jump to pdf position (e2e test needed)
- scroll pdf to update book layout highlight
- show/hide the drawer

---

## Story: Reading record

- mark a book range as started
- mark a book range as finished
- mark a book range as skipped
- mark a book range as skimmed
- reset the reading status of a book range

---

## Story: Reorganizing the book layout

- use AI to reorg the nesting

---

## Story: Decide or navigate to the next range to read

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