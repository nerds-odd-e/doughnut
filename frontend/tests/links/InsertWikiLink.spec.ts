import MakeMe from "doughnut-test-fixtures/makeMe"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import {
  insertedTexts,
  openLinkInsertionChoice,
  setupInsertWikiLinkTests,
  wikiPropertyInserted,
} from "@tests/links/insertWikiLinkTestSupport"
import { describe, expect, it } from "vitest"

describe("InsertWikiLink", () => {
  setupInsertWikiLinkTests()

  it("calls the registered inserter with a wiki link text when Insert as a wiki link is clicked", async () => {
    const note = MakeMe.aNote.please()
    const targetResult = MakeMe.aNoteSearchResult.title("Target CI").please()
    await openLinkInsertionChoice(note, {
      searchKey: "CI",
      targetResult,
    })

    expect(
      screen.queryByText("Add wiki link as a new property")
    ).not.toBeInTheDocument()

    fireEvent.click(screen.getByText("Insert as a wiki link"))
    await flushPromises()

    expect(insertedTexts).toContain("[[Target CI]]")
  })

  it("does not call the inserter when Add a new relationship note is clicked", async () => {
    const note = MakeMe.aNote.please()
    const targetResult = MakeMe.aNoteSearchResult.title("Sedation").please()
    await openLinkInsertionChoice(note, {
      searchKey: "Sed",
      targetResult,
      withRouter: true,
    })

    fireEvent.click(screen.getByText("Add a new relationship note"))
    await flushPromises()

    expect(insertedTexts).toHaveLength(0)
    expect(screen.getByText("Complete relationship")).toBeInTheDocument()
  })

  it("calls the wiki-property inserter when Add wiki link as a new property is clicked", async () => {
    const note = MakeMe.aNote.please()
    const targetResult = MakeMe.aNoteSearchResult.title("PropTarget").please()
    await openLinkInsertionChoice(note, {
      searchKey: "Prop",
      targetResult,
      wikiPropertyCanInsert: true,
    })

    fireEvent.click(screen.getByText("Add wiki link as a new property"))
    await flushPromises()

    expect(wikiPropertyInserted).toContain("[[PropTarget]]")
    expect(insertedTexts).toHaveLength(0)
  })
})
