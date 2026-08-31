import { beforeEach, describe, expect, it, vi } from "vitest"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import { notePropertyHref, noteShowHref } from "@/routes/noteShowLocation"
import { convertPastedNotePropertyLinksInNoteContent } from "@/utils/convertPastedNotePropertyLinks"

const moon = {
  noteTopology: { title: "Moon" },
  notebookId: 10,
  notebookName: "Sky",
}

describe("convertPastedNotePropertyLinksInNoteContent", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("encodes the property key from the route-decoded param", async () => {
    mockSdkService(NoteController, "authoredPortablePath", {
      portablePath: "Moon#prop:a%20part%20of",
    })
    const href = notePropertyHref(99, "a part of")
    expect(
      await convertPastedNotePropertyLinksInNoteContent(`[x](${href})`, {
        sourceNoteId: 1,
        sourceNotebookId: 10,
        resolveNote: async () => moon,
      })
    ).toBe("[[Moon#prop:a%20part%20of|x]]")
  })

  it("does not use a href-shaped label as display text", async () => {
    mockSdkService(NoteController, "authoredPortablePath", {
      portablePath: "Moon#prop:topic",
    })
    const href = notePropertyHref(99, "topic")
    expect(
      await convertPastedNotePropertyLinksInNoteContent(`[${href}](${href})`, {
        sourceNoteId: 1,
        sourceNotebookId: 10,
        resolveNote: async () => moon,
      })
    ).toBe("[[Moon#prop:topic]]")
  })

  it("calls the authoring operation with the source and destination note ids", async () => {
    const spy = mockSdkService(NoteController, "authoredPortablePath", {
      portablePath: "Moon#prop:topic",
    })
    const href = notePropertyHref(99, "topic")
    await convertPastedNotePropertyLinksInNoteContent(`[x](${href})`, {
      sourceNoteId: 7,
      sourceNotebookId: 10,
      resolveNote: async () => moon,
    })
    expect(spy).toHaveBeenCalledWith({
      path: { note: 7 },
      query: { destinationNote: 99, portablePath: "#prop:topic" },
    })
  })

  it("leaves a normal markdown link when identity cannot be resolved", async () => {
    const href = notePropertyHref(99, "topic")
    const markdown = `See [shown](${href})`
    expect(
      await convertPastedNotePropertyLinksInNoteContent(markdown, {
        sourceNoteId: 1,
        sourceNotebookId: 10,
        resolveNote: async () => undefined,
      })
    ).toBe(markdown)
  })

  it("does not convert note-show hrefs or invent identity from their labels", async () => {
    const markdown = `[Beta](${noteShowHref(99)})`
    expect(
      await convertPastedNotePropertyLinksInNoteContent(markdown, {
        sourceNoteId: 1,
        sourceNotebookId: 10,
        resolveNote: async () => moon,
      })
    ).toBe(markdown)
  })

  it("leaves verbatim frontmatter untouched", async () => {
    mockSdkService(NoteController, "authoredPortablePath", {
      portablePath: "Moon#prop:topic",
    })
    const href = notePropertyHref(99, "topic")
    const markdown = `---\nsee: "[hidden](${href})"\n---\nSee [shown](${href})\n`
    expect(
      await convertPastedNotePropertyLinksInNoteContent(markdown, {
        sourceNoteId: 1,
        sourceNotebookId: 10,
        resolveNote: async () => moon,
      })
    ).toBe(`---\nsee: "[hidden](${href})"\n---\nSee [[Moon#prop:topic|shown]]`)
  })
})
