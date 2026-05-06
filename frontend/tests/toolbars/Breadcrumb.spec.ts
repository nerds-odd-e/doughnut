import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import { screen } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { describe, it, expect } from "vitest"

describe("breadcrumb with circles", () => {
  it("view note belongs to other people in bazaar", async () => {
    helper
      .component(BreadcrumbWithCircle)
      .withProps({
        notebookView: {
          notebook: makeMe.aNotebook.please(),
          readonly: true,
        },
        ancestorFolders: [],
      })
      .render()
    await screen.findByText("Bazaar")
  })

  it("shows notebook name on the note breadcrumb when notebook is provided", async () => {
    const notebook = makeMe.aNotebook.please()
    helper
      .component(BreadcrumbWithCircle)
      .withProps({
        notebookView: { notebook, readonly: false },
        ancestorFolders: [],
      })
      .render()
    await screen.findByText("Notebooks")
    const notebookName = screen.getByText(notebook.name)
    expect(notebookName.closest("a")).not.toBeNull()
  })

  it("shows folder trail in outer-to-inner order via ancestorFolders", async () => {
    helper
      .component(BreadcrumbWithCircle)
      .withProps({
        notebookView: {
          notebook: makeMe.aNotebook.please(),
          readonly: false,
        },
        ancestorFolders: [
          { id: "1", name: "parent" },
          { id: "2", name: "child" },
        ],
      })
      .render()
    const parentEl = screen.getByText("parent")
    const childEl = screen.getByText("child")
    expect(
      parentEl.compareDocumentPosition(childEl) &
        Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy()
  })

  it("shows folder trail from ancestorFolders instead of parent topology", async () => {
    helper
      .component(BreadcrumbWithCircle)
      .withProps({
        notebookView: {
          notebook: makeMe.aNotebook.please(),
          readonly: false,
        },
        ancestorFolders: [
          { id: 10, name: "Outer" },
          { id: 20, name: "Inner" },
        ],
      })
      .render()
    expect(screen.queryByText("parent")).toBeNull()
    expect(screen.queryByText("child")).toBeNull()
    expect(screen.getByText("Outer")).toBeTruthy()
    expect(screen.getByText("Inner")).toBeTruthy()
    expect(
      screen
        .getByText("Outer")
        .compareDocumentPosition(screen.getByText("Inner")) &
        Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy()
  })
})
