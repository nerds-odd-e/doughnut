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

### Sub-stories

- User reads a top-level range that has child ranges.
- User reads a leaf range.
- User splits a range further.

---

## Story: Reading record

_(Sub-stories to be added.)_

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
