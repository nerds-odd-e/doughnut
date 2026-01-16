import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
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

  it("show ancestors in correct order", async () => {
    helper
      .component(BreadcrumbWithCircle)
      .withProps({ fromBazaar: false, noteTopology: grandChild.noteTopology })
      .render()
    const items = screen.getAllByText(/parent|child/)
    expect(items[0]).toHaveTextContent("parent")
    expect(items[1]).toHaveTextContent("child")
  })
})
