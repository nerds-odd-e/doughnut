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
        notebookRealm: {
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
        notebookRealm: { notebook, readonly: false },
        ancestorFolders: [],
      })
      .render()
    const notebookName = await screen.findByText(notebook.name)
    expect(notebookName.closest("a")).not.toBeNull()
  })

  it("shows folder trail in outer-to-inner order via ancestorFolders", async () => {
    helper
      .component(BreadcrumbWithCircle)
      .withProps({
        notebookRealm: {
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
        notebookRealm: {
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

  it("links each folder segment to folderPage for that folder", async () => {
    const notebook = makeMe.aNotebook.please()
    helper
      .component(BreadcrumbWithCircle)
      .withProps({
        notebookRealm: {
          notebook,
          readonly: false,
        },
        ancestorFolders: [
          { id: 10, name: "Outer" },
          { id: 20, name: "Inner" },
        ],
      })
      .render()
    const outerLink = screen.getByText("Outer").closest("a")
    const innerLink = screen.getByText("Inner").closest("a")
    expect(outerLink).not.toBeNull()
    expect(innerLink).not.toBeNull()
    expect(outerLink?.getAttribute("to")).toContain(`"name":"folderPage"`)
    expect(outerLink?.getAttribute("to")).toContain(`"folderId":"10"`)
    expect(outerLink?.getAttribute("to")).toContain(
      `"notebookId":"${notebook.id}"`
    )
    expect(innerLink?.getAttribute("to")).toContain(`"folderId":"20"`)
  })
})
