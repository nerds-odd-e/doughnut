import { describe, expect, it } from "vitest"
import { notePropertyHref, noteShowHref } from "@/routes/noteShowLocation"
import { convertPastedNotePropertyLinksInNoteContent } from "@/utils/convertPastedNotePropertyLinks"

const moon = {
  noteTopology: { title: "Moon" },
  notebookId: 10,
  notebookName: "Sky",
}

describe("convertPastedNotePropertyLinksInNoteContent", () => {
  it("encodes the property key from the route-decoded param", async () => {
    const href = notePropertyHref(99, "a part of")
    expect(
      await convertPastedNotePropertyLinksInNoteContent(`[x](${href})`, {
        sourceNotebookId: 10,
        resolveNote: async () => moon,
      })
    ).toBe("[[Moon#prop:a%20part%20of|x]]")
  })

  it("does not use a href-shaped label as display text", async () => {
    const href = notePropertyHref(99, "topic")
    expect(
      await convertPastedNotePropertyLinksInNoteContent(`[${href}](${href})`, {
        sourceNotebookId: 10,
        resolveNote: async () => moon,
      })
    ).toBe("[[Moon#prop:topic]]")
  })

  it("leaves a normal markdown link when identity cannot be resolved", async () => {
    const href = notePropertyHref(99, "topic")
    const markdown = `See [shown](${href})`
    expect(
      await convertPastedNotePropertyLinksInNoteContent(markdown, {
        sourceNotebookId: 10,
        resolveNote: async () => undefined,
      })
    ).toBe(markdown)
  })

  it("does not convert note-show hrefs or invent identity from their labels", async () => {
    const markdown = `[Beta](${noteShowHref(99)})`
    expect(
      await convertPastedNotePropertyLinksInNoteContent(markdown, {
        sourceNotebookId: 10,
        resolveNote: async () => moon,
      })
    ).toBe(markdown)
  })

  it("leaves verbatim frontmatter untouched", async () => {
    const href = notePropertyHref(99, "topic")
    const markdown = `---\nsee: "[hidden](${href})"\n---\nSee [shown](${href})\n`
    expect(
      await convertPastedNotePropertyLinksInNoteContent(markdown, {
        sourceNotebookId: 10,
        resolveNote: async () => moon,
      })
    ).toBe(`---\nsee: "[hidden](${href})"\n---\nSee [[Moon#prop:topic|shown]]`)
  })
})
