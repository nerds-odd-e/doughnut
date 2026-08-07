import makeMe from "doughnut-test-fixtures/makeMe"
import { wikiTitleFromInnerAndNoteId } from "@/utils/wikiPropertyValueField"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import { afterEach, describe, expect, it, vi } from "vitest"
import type { ComponentPublicInstance } from "vue"
import { mountNoteTextContent } from "./noteTextContentTestSupport"

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
          wikiTitleFromInnerAndNoteId(
            "Target Title|friendly label",
            targetNote.id!
          ),
        ],
      }
    )
    await flushPromises()
    await vi.waitUntil(() =>
      document.querySelector(".ql-editor a.doughnut-wiki-link")
    )
    const live = document.querySelector(
      ".ql-editor a.doughnut-wiki-link"
    ) as HTMLAnchorElement
    expect(live.textContent).toContain("friendly label")
    expect(live.textContent).not.toContain("Target Title|")
    expect(live.getAttribute("href")).toBe(`/n${targetNote.id}`)
    expect(live.getAttribute("data-wiki-title")).toBe("Target Title")
  })
})
