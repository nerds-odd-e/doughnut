import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import NoteContextReader from "@/components/notes/NoteContextReader.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { afterEach, describe, expect, it, vi } from "vitest"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { wikiLinkFromAuthoredToken } from "@/utils/authoredLinkMarkup"
import type { NoteRealm } from "@generated/donut-backend-api"

describe("NoteContextReader", () => {
  afterEach(() => {
    document.body.innerHTML = ""
  })

  const mountReader = async (noteRealm: NoteRealm) => {
    mockSdkService(NoteController, "showNote", noteRealm)
    const wrapper = helper
      .component(NoteContextReader)
      .withRouter()
      .withCleanStorage()
      .withProps({ noteId: noteRealm.id })
      .mount({ attachTo: document.body })
    await flushPromises()
    return wrapper
  }

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

    const wrapper = await mountReader(noteRealm)

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

  it("shows the note image, usable wiki links, and every inbound reference", async () => {
    const noteRealm = makeMe.aNoteRealm
      .content(`---
image: https://example.com/a.png
---

See [[Treason]].`)
      .wikiLinks([wikiLinkFromAuthoredToken("Treason", 77)])
      .please()
    makeMe.aNoteRealm.title("Mutiny").under(noteRealm).please()
    makeMe.aNoteRealm.title("Insurrection").under(noteRealm).please()

    const wrapper = await mountReader(noteRealm)

    expect(wrapper.find("#note-image img").attributes("src")).toBe(
      "https://example.com/a.png"
    )
    const references = wrapper.text()
    expect(references).toContain("References")
    expect(references).toContain("Mutiny")
    expect(references).toContain("Insurrection")

    await vi.waitUntil(() =>
      document.querySelector(".ql-editor a.donut-wiki-link")
    )
    const router = wrapper.vm.$router
    const pushSpy = vi.spyOn(router, "push").mockResolvedValue(undefined)
    ;(
      document.querySelector(
        ".ql-editor a.donut-wiki-link"
      ) as HTMLAnchorElement
    ).click()
    expect(pushSpy).toHaveBeenCalledWith(noteShowLocation(77))
    wrapper.unmount()
  })

  it("leaves out the references section when nothing references the note", async () => {
    const wrapper = await mountReader(makeMe.aNoteRealm.please())

    expect(wrapper.text()).not.toContain("References")
    wrapper.unmount()
  })
})
