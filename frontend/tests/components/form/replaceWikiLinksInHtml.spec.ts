import { replaceWikiLinksInHtml } from "@/components/form/replaceWikiLinksInHtml"
import { noteShowHref } from "@/routes/noteShowLocation"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import type { WikiLink } from "@generated/donut-backend-api"
import { describe, it, expect } from "vitest"

describe("replaceWikiLinksInHtml", () => {
  it("replaces known wikilink text with a note href", () => {
    expect(
      replaceWikiLinksInHtml("<p>[[MyNote]]</p>", [
        wikiLinkFromAuthoredToken("MyNote", 42),
      ])
    ).toBe(
      `<p><a href="${noteShowHref(42)}" class="donut-wiki-link" data-portable-path="MyNote" data-note-id="42">MyNote</a></p>`
    )
  })

  it("replaces piped wikilink with display text as anchor body", () => {
    expect(
      replaceWikiLinksInHtml("<p>[[Target|label]]</p>", [
        wikiLinkFromAuthoredToken("Target|label", 7),
      ])
    ).toBe(
      `<p><a href="${noteShowHref(7)}" class="donut-wiki-link" data-portable-path="Target" data-display-text="label" data-note-id="7">label</a></p>`
    )
  })

  it("replaces every occurrence when the same wikilink appears multiple times", () => {
    const html = "<p>[[MyNote]] then [[MyNote]]</p>"
    const out = replaceWikiLinksInHtml(html, [
      wikiLinkFromAuthoredToken("MyNote", 42),
    ])
    expect(out).not.toContain("[[MyNote]]")
    expect(out).toMatch(/donut-wiki-link[\s\S]* then [\s\S]*donut-wiki-link/)
  })

  it("does not treat AMBIGUOUS wikiLinks as live destinations", () => {
    const ambiguous: WikiLink = {
      authoredLink: "Shared",
      target: "Shared",
      displayText: "Shared",
      resolution: "AMBIGUOUS",
    }
    expect(replaceWikiLinksInHtml("<p>[[Shared]]</p>", [ambiguous])).toBe(
      '<p><a href="#" class="dead-wiki-link" data-portable-path="Shared" data-resolution="AMBIGUOUS">Shared</a></p>'
    )
  })

  it("marks unknown wikilinks as dead links", () => {
    expect(replaceWikiLinksInHtml("<p>[[Unknown]]</p>", [])).toBe(
      '<p><a href="#" class="dead-wiki-link" data-portable-path="Unknown">Unknown</a></p>'
    )
  })

  it("leaves ordinary file-looking Markdown anchors unchanged", () => {
    expect(
      replaceWikiLinksInHtml('<p><a href="/Folder/Missing.md">label</a></p>', [
        {
          authoredLink: "[label](/Folder/Missing.md)",
          target: "/Folder/Missing.md",
          displayText: "label",
          resolution: "RESOLVED",
          destinationNoteId: 42,
        },
      ])
    ).toBe('<p><a href="/Folder/Missing.md">label</a></p>')
  })

  it("preserves Quill hr markup without rewriting through DOMParser", () => {
    const quillHr = "<p><hr></p>"
    expect(replaceWikiLinksInHtml(quillHr, [])).toBe(quillHr)
  })

  it("is idempotent for Quill hr markup", () => {
    const quillHr = "<p><hr></p>"
    const once = replaceWikiLinksInHtml(quillHr, [])
    const twice = replaceWikiLinksInHtml(once, [])
    expect(twice).toBe(once)
  })

  it("upgrades rich-editor dead-wiki-link anchors when wikiLinks resolve", () => {
    const out = replaceWikiLinksInHtml(
      '<p><a href="#" class="dead-wiki-link">MyNote</a></p>',
      [wikiLinkFromAuthoredToken("MyNote", 42)]
    )
    expect(out).toContain("donut-wiki-link")
    expect(out).not.toContain("dead-wiki-link")
  })

  it("marks a new token pending when it is absent from last-saved markdown", () => {
    expect(
      replaceWikiLinksInHtml("<p>[[WikiLinks E2E Nowhere]]</p>", [], "Saved.")
    ).toBe(
      '<p><a href="#" class="pending-wiki-link" data-portable-path="WikiLinks E2E Nowhere">WikiLinks E2E Nowhere</a></p>'
    )
  })

  it("marks a last-saved unmatched token dead", () => {
    expect(
      replaceWikiLinksInHtml(
        "<p>[[WikiLinks E2E Already Missing]]</p>",
        [],
        "Saved [[WikiLinks E2E Already Missing]]."
      )
    ).toBe(
      '<p><a href="#" class="dead-wiki-link" data-portable-path="WikiLinks E2E Already Missing">WikiLinks E2E Already Missing</a></p>'
    )
  })

  it("keeps a wikiLinks hit live even when last-saved markdown is provided", () => {
    const out = replaceWikiLinksInHtml(
      "<p>[[MyNote]]</p>",
      [wikiLinkFromAuthoredToken("MyNote", 42)],
      "[[MyNote]]"
    )
    expect(out).toContain("donut-wiki-link")
    expect(out).not.toContain("pending-wiki-link")
    expect(out).not.toContain("dead-wiki-link")
  })

  it("turns a pending anchor dead once the token is in last-saved markdown", () => {
    expect(
      replaceWikiLinksInHtml(
        '<p><a href="#" class="pending-wiki-link" data-portable-path="WikiLinks E2E Nowhere">WikiLinks E2E Nowhere</a></p>',
        [],
        "See [[WikiLinks E2E Nowhere]]."
      )
    ).toBe(
      '<p><a href="#" class="dead-wiki-link" data-portable-path="WikiLinks E2E Nowhere">WikiLinks E2E Nowhere</a></p>'
    )
  })

  it("upgrades a last-saved pending anchor to live when wikiLinks resolve", () => {
    const out = replaceWikiLinksInHtml(
      '<p><a href="#" class="pending-wiki-link" data-portable-path="MyNote">MyNote</a></p>',
      [wikiLinkFromAuthoredToken("MyNote", 42)],
      "[[MyNote]]"
    )
    expect(out).toContain("donut-wiki-link")
    expect(out).not.toContain("pending-wiki-link")
  })
})
