import { beforeEach, describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  catalogHeadingTexts,
  clearNotebooksPageStorage,
  emitNotebookUpdated,
  mockMyNotebooks,
  mountNotebooksPage,
} from "./notebooksPageTestSupport"

describe("notebook updates", () => {
  beforeEach(() => {
    clearNotebooksPageStorage()
  })

  it("updates the catalog title when notebook-updated fires", async () => {
    const originalNotebook = makeMe.aNotebook.title("Original Title").please()
    const updatedNotebook = { ...originalNotebook, name: "Updated Title" }

    mockMyNotebooks({ notebooks: [{ notebook: originalNotebook }] })
    const wrapper = await mountNotebooksPage()
    expect(catalogHeadingTexts(wrapper)).toContain("Original Title")

    await emitNotebookUpdated(wrapper, updatedNotebook)
    expect(catalogHeadingTexts(wrapper)).toEqual(["Updated Title"])
  })

  it("patches only the matching notebook when the catalog has several", async () => {
    const notebook1 = makeMe.aNotebook.title("Notebook 1").please()
    const notebook2 = makeMe.aNotebook.title("Notebook 2").please()
    const updatedNotebook1 = { ...notebook1, name: "Updated Notebook 1" }

    mockMyNotebooks({
      notebooks: [{ notebook: notebook1 }, { notebook: notebook2 }],
    })

    const wrapper = await mountNotebooksPage()
    await emitNotebookUpdated(wrapper, updatedNotebook1)

    expect(catalogHeadingTexts(wrapper)).toEqual([
      "Notebook 2",
      "Updated Notebook 1",
    ])
  })

  it("shows the empty catalog message when there are no notebooks", async () => {
    mockMyNotebooks({ notebooks: [], catalogItems: [] })

    const wrapper = await mountNotebooksPage()
    expect(wrapper.text()).toContain("You do not have any notebooks yet.")
  })
})
