import MakeMe from "donut-test-fixtures/makeMe"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import {
  insertedTexts,
  openWikiLinkOrRelationshipChoice,
  setupInsertWikiLinkTests,
  insertedWikiLinkAsProperty,
} from "@tests/wiki-link-or-relationship/insertWikiLinkTestSupport"
import { describe, expect, it } from "vitest"

describe("InsertWikiLink", () => {
  setupInsertWikiLinkTests()

  it("inserts the backend-authored Portable path", async () => {
    const portablePath = "Folder/Target CI"
    mockSdkService(NoteController, "authoredPortablePath", { portablePath })
    const note = MakeMe.aNote.please()
    const targetResult = MakeMe.aNoteSearchResult.title("Target CI").please()
    await openWikiLinkOrRelationshipChoice(note, {
      searchKey: "CI",
      targetResult,
    })

    expect(
      screen.queryByText("Add wiki link as a new property")
    ).not.toBeInTheDocument()

    fireEvent.click(screen.getByText("Insert as a wiki link"))
    await flushPromises()

    expect(insertedTexts).toContain(`[[${portablePath}]]`)
  })

  it("does not call the inserter when Add a new relationship note is clicked", async () => {
    const note = MakeMe.aNote.please()
    const targetResult = MakeMe.aNoteSearchResult.title("Sedation").please()
    await openWikiLinkOrRelationshipChoice(note, {
      searchKey: "Sed",
      targetResult,
      withRouter: true,
    })

    fireEvent.click(screen.getByText("Add a new relationship note"))
    await flushPromises()

    expect(insertedTexts).toHaveLength(0)
    expect(screen.getByText("Complete relationship")).toBeInTheDocument()
  })

  it("calls the insert-wiki-link-as-property inserter with the backend-authored Portable path", async () => {
    mockSdkService(NoteController, "authoredPortablePath", {
      portablePath: "Folder/PropTarget",
    })
    const note = MakeMe.aNote.please()
    const targetResult = MakeMe.aNoteSearchResult.title("PropTarget").please()
    await openWikiLinkOrRelationshipChoice(note, {
      searchKey: "Prop",
      targetResult,
      canInsertWikiLinkAsProperty: true,
    })

    fireEvent.click(screen.getByText("Add wiki link as a new property"))
    await flushPromises()

    expect(insertedWikiLinkAsProperty).toContain("[[Folder/PropTarget]]")
    expect(insertedTexts).toHaveLength(0)
  })
})
