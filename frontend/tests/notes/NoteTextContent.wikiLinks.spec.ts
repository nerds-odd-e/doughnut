import makeMe from "donut-test-fixtures/makeMe"
import htmlToMarkdown from "@/components/form/quillHtmlToMarkdown"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
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
    expect(dead.getAttribute("data-portable-path")).toBe("Unknown Topic")
  })

  it("shows display text for resolved body wiki link", async () => {
    const targetNote = makeMe.aNote.title("Target Title").please()
    wrapper = mountNoteTextContent(
      makeMe.aNote.content("Go [[Target Title|friendly label]] ok.").please(),
      {
        readonly: true,
        wikiLinks: [
          wikiLinkFromAuthoredToken(
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
    expect(live.getAttribute("data-portable-path")).toBe("Target Title")
  })

  it("keeps file-looking Markdown URLs as ordinary anchors through render and serialize", async () => {
    wrapper = mountNoteTextContent(
      makeMe.aNote.content("See [Target](/folder/Target.md).").please(),
      {
        readonly: true,
        wikiLinks: [
          {
            authoredLink: "[Target](/folder/Target.md)",
            target: "/folder/Target.md",
            displayText: "Target",
            resolution: "RESOLVED",
            destinationNoteId: 99,
          },
        ],
      }
    )
    await flushPromises()
    await vi.waitUntil(() => document.querySelector(".ql-editor a"))
    const anchor = document.querySelector(".ql-editor a") as HTMLAnchorElement
    expect(anchor.classList.contains("donut-wiki-link")).toBe(false)
    expect(anchor.classList.contains("dead-wiki-link")).toBe(false)
    expect(anchor.classList.contains("pending-wiki-link")).toBe(false)
    expect(anchor.getAttribute("href")).toBe("/folder/Target.md")
    expect(anchor.textContent).toContain("Target")
    expect(
      htmlToMarkdown(document.querySelector(".ql-editor")!.innerHTML)
    ).toContain("[Target](/folder/Target.md)")
  })

  it("shows a new wiki link as pending until content save confirms it is missing", async () => {
    const savedContent = "Saved [[WikiLinks E2E Already Missing]]."
    const note = makeMe.aNote
      .title("Wiki carrier")
      .content(savedContent)
      .please()
    const inFlightContent = `${savedContent} See [[WikiLinks E2E Nowhere]].`

    const releaseSave = holdNoteContentSave((content) =>
      makeMe.aNoteRealm.id(note.id!).content(content).wikiLinks([]).please()
    )

    wrapper = mountNoteTextContent(note, { readonly: false, wikiLinks: [] })
    await flushPromises()
    await vi.waitUntil(() =>
      document.querySelector(".ql-editor a.dead-wiki-link")
    )

    wrapper
      .findComponent({ name: "QuillEditor" })
      .vm.$emit(
        "update:modelValue",
        `<p>Saved <a href="#" class="dead-wiki-link" data-portable-path="WikiLinks E2E Already Missing">WikiLinks E2E Already Missing</a>. See [[WikiLinks E2E Nowhere]].</p>`
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
      wikiLinks: [],
    })
    await flushPromises()
    await vi.waitUntil(
      () =>
        document.querySelectorAll(".ql-editor a.dead-wiki-link").length === 2
    )

    expect(document.querySelector(".ql-editor a.pending-wiki-link")).toBeNull()
    const deadTitles = [
      ...document.querySelectorAll(".ql-editor a.dead-wiki-link"),
    ].map((a) => a.getAttribute("data-portable-path"))
    expect(deadTitles).toContain("WikiLinks E2E Already Missing")
    expect(deadTitles).toContain("WikiLinks E2E Nowhere")
  })

  it("shows a new wiki link to an existing note as live after content save", async () => {
    const savedContent = "Saved."
    const note = makeMe.aNote.content(savedContent).please()
    const liveWikiLinks = [wikiLinkFromAuthoredToken("WikiLinks E2E CI", 42)]
    const inFlightContent = `${savedContent} See [[WikiLinks E2E CI]].`
    const contentSaved = vi.fn((content: string) =>
      makeMe.aNoteRealm
        .id(note.id!)
        .content(content)
        .wikiLinks(liveWikiLinks)
        .please()
    )
    const releaseSave = holdNoteContentSave(contentSaved)

    wrapper = mountNoteTextContent(note, { readonly: false, wikiLinks: [] })
    await flushPromises()

    wrapper
      .findComponent({ name: "QuillEditor" })
      .vm.$emit("update:modelValue", `<p>Saved. See [[WikiLinks E2E CI]].</p>`)

    releaseSave()
    await flushPromises()
    expect(contentSaved).toHaveBeenCalledWith(inFlightContent)

    await wrapper.setProps({
      note: makeMe.aNote.id(note.id!).content(inFlightContent).please(),
      wikiLinks: liveWikiLinks,
    })
    await flushPromises()

    const live = wrapper.get(".ql-editor a.donut-wiki-link")
    expect(live.attributes("data-portable-path")).toBe("WikiLinks E2E CI")
  })
})
