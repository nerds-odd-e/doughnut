import makeMe from "donut-test-fixtures/makeMe"
import { noteShowHref } from "@/routes/noteShowLocation"
import { wikiTitleFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import { afterEach, describe, expect, it, vi } from "vitest"
import type { ComponentPublicInstance } from "vue"
import {
  holdNoteContentSave,
  mountNoteTextContent,
} from "./noteTextContentTestSupport"

describe("NoteTextContent wiki link display", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("shows display text after pipe for unresolved body wiki link", async () => {
    wrapper = mountNoteTextContent(
      makeMe.aNote
        .title("Wiki test")
        .content("Intro [[Unknown Topic|friendly label]] out.")
        .please(),
      { readonly: true }
    )
    await flushPromises()
    await vi.waitUntil(() =>
      document.querySelector(".ql-editor a.dead-wiki-link")
    )
    const dead = document.querySelector(
      ".ql-editor a.dead-wiki-link"
    ) as HTMLAnchorElement
    expect(dead.textContent).toContain("friendly label")
    expect(dead.textContent).not.toContain("Unknown Topic|")
    expect(dead.getAttribute("data-wiki-title")).toBe("Unknown Topic")
  })

  it("shows display text for resolved body wiki link", async () => {
    const targetNote = makeMe.aNote.title("Target Title").please()
    wrapper = mountNoteTextContent(
      makeMe.aNote.content("Go [[Target Title|friendly label]] ok.").please(),
      {
        readonly: true,
        wikiTitles: [
          wikiTitleFromAuthoredToken(
            "Target Title|friendly label",
            targetNote.id!
          ),
        ],
      }
    )
    await flushPromises()
    await vi.waitUntil(() =>
      document.querySelector(".ql-editor a.donut-wiki-link")
    )
    const live = document.querySelector(
      ".ql-editor a.donut-wiki-link"
    ) as HTMLAnchorElement
    expect(live.textContent).toContain("friendly label")
    expect(live.textContent).not.toContain("Target Title|")
    expect(live.getAttribute("href")).toBe(noteShowHref(targetNote.id!))
    expect(live.getAttribute("data-note-id")).toBe(String(targetNote.id))
    expect(live.getAttribute("data-wiki-title")).toBe("Target Title")
  })

  it("shows unresolved path markdown as a dead wiki link", async () => {
    wrapper = mountNoteTextContent(
      makeMe.aNote.content("See [label](/Folder/Missing.md).").please(),
      { readonly: true }
    )
    await flushPromises()
    await vi.waitUntil(() =>
      document.querySelector(".ql-editor a.dead-wiki-link")
    )
    const dead = document.querySelector(
      ".ql-editor a.dead-wiki-link"
    ) as HTMLAnchorElement
    expect(dead.textContent).toContain("label")
    expect(dead.getAttribute("href")).toBe("#")
    expect(dead.getAttribute("data-wiki-title")).toBe("/Folder/Missing.md")
  })

  it("shows a path markdown link as a live wiki-style link to the note", async () => {
    const targetNote = makeMe.aNote.title("Title").please()
    wrapper = mountNoteTextContent(
      makeMe.aNote.content("See [label](/Folder/Title.md).").please(),
      {
        readonly: true,
        wikiTitles: [
          wikiTitleFromAuthoredToken(
            "[label](/Folder/Title.md)",
            targetNote.id!
          ),
        ],
      }
    )
    await flushPromises()
    await vi.waitUntil(() =>
      document.querySelector(".ql-editor a.donut-wiki-link")
    )
    const live = document.querySelector(
      ".ql-editor a.donut-wiki-link"
    ) as HTMLAnchorElement
    expect(live.textContent).toContain("label")
    expect(live.getAttribute("href")).toBe(noteShowHref(targetNote.id!))
    expect(live.getAttribute("data-note-id")).toBe(String(targetNote.id))
  })

  it("shows a new wiki link as pending until content save confirms it is missing", async () => {
    const savedContent = "Saved [[WikiLinks E2E Already Missing]]."
    const note = makeMe.aNote
      .title("Wiki carrier")
      .content(savedContent)
      .please()
    const inFlightContent = `${savedContent} See [[WikiLinks E2E Nowhere]].`

    const releaseSave = holdNoteContentSave((content) =>
      makeMe.aNoteRealm.id(note.id!).content(content).wikiTitles([]).please()
    )

    wrapper = mountNoteTextContent(note, { readonly: false, wikiTitles: [] })
    await flushPromises()
    await vi.waitUntil(() =>
      document.querySelector(".ql-editor a.dead-wiki-link")
    )

    wrapper
      .findComponent({ name: "QuillEditor" })
      .vm.$emit(
        "update:modelValue",
        `<p>Saved <a href="#" class="dead-wiki-link" data-wiki-title="WikiLinks E2E Already Missing">WikiLinks E2E Already Missing</a>. See [[WikiLinks E2E Nowhere]].</p>`
      )
    await flushPromises()
    await vi.waitUntil(() =>
      document.querySelector(".ql-editor a.pending-wiki-link")
    )

    const pending = document.querySelector(
      ".ql-editor a.pending-wiki-link"
    ) as HTMLAnchorElement
    expect(pending.textContent).toContain("WikiLinks E2E Nowhere")
    expect(
      document.querySelector(".ql-editor a.dead-wiki-link")?.textContent
    ).toContain("WikiLinks E2E Already Missing")

    releaseSave()
    await flushPromises()
    await wrapper.setProps({
      note: makeMe.aNote
        .id(note.id!)
        .title("Wiki carrier")
        .content(inFlightContent)
        .please(),
      wikiTitles: [],
    })
    await flushPromises()
    await vi.waitUntil(
      () =>
        document.querySelectorAll(".ql-editor a.dead-wiki-link").length === 2
    )

    expect(document.querySelector(".ql-editor a.pending-wiki-link")).toBeNull()
    const deadTitles = [
      ...document.querySelectorAll(".ql-editor a.dead-wiki-link"),
    ].map((a) => a.getAttribute("data-wiki-title"))
    expect(deadTitles).toContain("WikiLinks E2E Already Missing")
    expect(deadTitles).toContain("WikiLinks E2E Nowhere")
  })

  it("shows a new wiki link to an existing note as live after content save", async () => {
    const savedContent = "Saved."
    const note = makeMe.aNote
      .title("Wiki carrier")
      .content(savedContent)
      .please()
    const targetNote = makeMe.aNote.title("WikiLinks E2E CI").please()
    const liveWikiTitles = [
      wikiTitleFromAuthoredToken("WikiLinks E2E CI", targetNote.id!),
    ]
    const inFlightContent = `${savedContent} See [[WikiLinks E2E CI]].`

    const releaseSave = holdNoteContentSave((content) =>
      makeMe.aNoteRealm
        .id(note.id!)
        .content(content)
        .wikiTitles(liveWikiTitles)
        .please()
    )

    wrapper = mountNoteTextContent(note, { readonly: false, wikiTitles: [] })
    await flushPromises()

    wrapper
      .findComponent({ name: "QuillEditor" })
      .vm.$emit("update:modelValue", `<p>Saved. See [[WikiLinks E2E CI]].</p>`)
    await flushPromises()
    await vi.waitUntil(() =>
      document.querySelector(".ql-editor a.pending-wiki-link")
    )

    releaseSave()
    await flushPromises()
    await wrapper.setProps({
      note: makeMe.aNote
        .id(note.id!)
        .title("Wiki carrier")
        .content(inFlightContent)
        .please(),
      wikiTitles: liveWikiTitles,
    })
    await flushPromises()
    await vi.waitUntil(() =>
      document.querySelector(".ql-editor a.donut-wiki-link")
    )

    const live = document.querySelector(
      ".ql-editor a.donut-wiki-link"
    ) as HTMLAnchorElement
    expect(live.getAttribute("data-wiki-title")).toBe("WikiLinks E2E CI")
    expect(live.getAttribute("href")).toBe(noteShowHref(targetNote.id!))
  })
})
