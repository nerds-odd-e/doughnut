import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import { screen } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { describe, it, expect } from "vitest"

describe("breadcrumb with circles", () => {
  const parentNote = makeMe.aNote.title("parent").please()
  const child = makeMe.aNote.title("child").underNote(parentNote).please()
  const grandChild = makeMe.aNote.underNote(child).please()

  it("view note belongs to other people in bazaar", async () => {
    helper
      .component(BreadcrumbWithCircle)
      .withProps({ fromBazaar: true, noteTopology: parentNote.noteTopology })
      .render()
    await screen.findByText("Bazaar")
  })

  it("shows folder trail in outer-to-inner order via ancestorFolders", async () => {
    helper
      .component(BreadcrumbWithCircle)
      .withProps({
        fromBazaar: false,
        noteTopology: grandChild.noteTopology,
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
        fromBazaar: false,
        noteTopology: grandChild.noteTopology,
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
