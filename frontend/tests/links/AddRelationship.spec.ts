import AddRelationshipFinalize from "@/components/links/AddRelationshipFinalize.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import type { NoteSearchResult } from "@generated/doughnut-backend-api"
import helper from "@tests/helpers"
import { describe, it, expect } from "vitest"

describe("AddRelationshipFinalize", () => {
  it("shows placement options with relations subfolder selected by default", async () => {
    const note = makeMe.aNote.please()
    const targetSearchResult: NoteSearchResult = makeMe.aNoteSearchResult
      .title("Target")
      .notebookId(1)
      .do()
    const wrapper = helper
      .component(AddRelationshipFinalize)
      .withCleanStorage()
      .withProps({
        note,
        targetSearchResult,
      })
      .mount()
    expect(wrapper.find('[role="radiogroup"]').exists()).toBe(true)
    const defaultRadio = wrapper.find(
      "#relationship-placement-relations_subfolder"
    )
    expect((defaultRadio.element as HTMLInputElement).checked).toBe(true)
  })

  it("going back", async () => {
    const note = makeMe.aNote.please()
    const targetSearchResult: NoteSearchResult = makeMe.aNoteSearchResult
      .title("Target")
      .notebookId(1)
      .do()
    const wrapper = helper
      .component(AddRelationshipFinalize)
      .withCleanStorage()
      .withProps({
        note,
        targetSearchResult,
      })
      .mount()
    await wrapper.find(".go-back-button").trigger("click")
    expect(wrapper.emitted().goBack).toHaveLength(1)
  })
})
