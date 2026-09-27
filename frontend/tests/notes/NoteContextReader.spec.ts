import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import NoteContextReader from "@/components/notes/NoteContextReader.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { afterEach, describe, expect, it } from "vitest"
import { noteShowLocation } from "@/routes/noteShowLocation"

describe("NoteContextReader", () => {
  afterEach(() => {
    document.body.innerHTML = ""
  })

  it("shows the note's title, location, properties, and body read-only", async () => {
    const noteRealm = makeMe.aNoteRealm
      .title("Sedition")
      .notebookName("Vocabulary")
      .inFolder(3, "Law words")
      .content(`---
topic: training
venue: online
---

Inciting rebellion against authority.`)
      .please()
    mockSdkService(NoteController, "showNote", noteRealm)

    const wrapper = helper
      .component(NoteContextReader)
      .withRouter()
      .withCleanStorage()
      .withProps({ noteId: noteRealm.id })
      .mount({ attachTo: document.body })
    await flushPromises()

    const text = wrapper.text()
    for (const expected of [
      "Sedition",
      "Vocabulary",
      "Law words",
      "topic",
      "training",
      "venue",
      "online",
      "Inciting rebellion against authority.",
    ]) {
      expect(text).toContain(expected)
    }
    const fullNoteLink = wrapper
      .findAll("a")
      .find((a) => a.text() === "Open full note")
    expect(fullNoteLink?.attributes("to")).toBe(
      JSON.stringify(noteShowLocation(noteRealm.id))
    )
    expect(wrapper.find("[data-note-toolbar]").exists()).toBe(false)
    expect(wrapper.find('[contenteditable="true"]').exists()).toBe(false)
    wrapper.unmount()
  })
})
