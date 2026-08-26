import { beforeEach, describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import {
  catalogHeadingTexts,
  clearNotebooksPageStorage,
  emitNotebookUpdated,
  mockMyNotebooks,
  mountNotebooksPage,
} from "./notebooksPageTestSupport"

describe("notebook updates for grouped catalog", () => {
  beforeEach(() => {
    clearNotebooksPageStorage()
  })

  it("patches grouped notebook title when notebook-updated fires", async () => {
    const member = makeMe.aNotebook.title("Member Title").please()
    const catalogItems = [
      makeMe.notebookCatalogGroup
        .id(1)
        .name("G")
        .createdAt("2020-01-01T00:00:00.000Z")
        .membersFromNotebooks([member])
        .please(),
    ]

    mockMyNotebooks({
      notebooks: [{ notebook: member }],
      catalogItems,
    })

    const wrapper = await mountNotebooksPage()
    await emitNotebookUpdated(wrapper, { ...member, name: "Renamed Member" })

    expect(catalogHeadingTexts(wrapper)).toContain("Renamed Member")
  })

  it("preserves hasAttachedBook when notebook-updated payload omits it", async () => {
    const notebookEntity = makeMe.aNotebook.title("T").please()

    mockMyNotebooks({
      notebooks: [{ notebook: notebookEntity, hasAttachedBook: true }],
    })

    const wrapper = await mountNotebooksPage()
    expect(
      wrapper.find('[data-testid="notebook-catalog-read-book"]').exists()
    ).toBe(true)

    await emitNotebookUpdated(wrapper, {
      ...notebookEntity,
      name: "Updated",
    })

    expect(
      wrapper.find('[data-testid="notebook-catalog-read-book"]').exists()
    ).toBe(true)
    expect(catalogHeadingTexts(wrapper)).toContain("Updated")
  })
})
