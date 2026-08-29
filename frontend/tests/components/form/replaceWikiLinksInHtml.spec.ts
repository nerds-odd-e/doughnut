import { replaceWikiLinksInHtml } from "@/components/form/replaceWikiLinksInHtml"
import { noteShowHref } from "@/routes/noteShowLocation"
import { wikiTitleFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { describe, it, expect } from "vitest"

describe("replaceWikiLinksInHtml", () => {
  it("replaces known wikilink text with a note href", () => {
    expect(
      replaceWikiLinksInHtml("<p>[[MyNote]]</p>", [
        wikiTitleFromAuthoredToken("MyNote", 42),
      ])
    ).toBe(
      `<p><a href="${noteShowHref(42)}" class="donut-wiki-link" data-wiki-title="MyNote" data-note-id="42">MyNote</a></p>`
    )
  })

  it("replaces piped wikilink with display text as anchor body", () => {
    expect(
      replaceWikiLinksInHtml("<p>[[Target|label]]</p>", [
        wikiTitleFromAuthoredToken("Target|label", 7),
      ])
    ).toBe(
      `<p><a href="${noteShowHref(7)}" class="donut-wiki-link" data-wiki-title="Target" data-wiki-display="label" data-note-id="7">label</a></p>`
    )
  })

  it("replaces every occurrence when the same wikilink appears multiple times", () => {
    const html = "<p>[[MyNote]] then [[MyNote]]</p>"
    const out = replaceWikiLinksInHtml(html, [
      wikiTitleFromAuthoredToken("MyNote", 42),
    ])
    expect(out).not.toContain("dead-wiki-link")
    expect(out).toBe(
      `<p><a href="${noteShowHref(42)}" class="donut-wiki-link" data-wiki-title="MyNote" data-note-id="42">MyNote</a> then <a href="${noteShowHref(42)}" class="donut-wiki-link" data-wiki-title="MyNote" data-note-id="42">MyNote</a></p>`
    )
  })

  it("marks unknown wikilinks as dead links", () => {
    expect(replaceWikiLinksInHtml("<p>[[Unknown]]</p>", [])).toBe(
      '<p><a href="#" class="dead-wiki-link" data-wiki-title="Unknown">Unknown</a></p>'
    )
  })

  it("upgrades dead path markdown anchors to live when wikiTitles resolve", () => {
    expect(
      replaceWikiLinksInHtml(
        '<p><a href="/Folder/Title.md" class="dead-wiki-link" data-wiki-title="/Folder/Title.md" data-wiki-display="label">label</a></p>',
        [wikiTitleFromAuthoredToken("[label](/Folder/Title.md)", 42)]
      )
    ).toBe(
      '<p><a href="/Folder/Title.md" class="donut-wiki-link" data-wiki-title="/Folder/Title.md" data-wiki-display="label" data-note-id="42">label</a></p>'
    )
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

  it("upgrades rich-editor dead-wiki-link anchors when wikiTitles resolve", () => {
    expect(
      replaceWikiLinksInHtml(
        '<p><a href="#" class="dead-wiki-link">MyNote</a></p>',
        [wikiTitleFromAuthoredToken("MyNote", 42)]
      )
    ).toBe(
      `<p><a href="${noteShowHref(42)}" class="donut-wiki-link" data-wiki-title="MyNote" data-note-id="42">MyNote</a></p>`
    )
  })

  it("marks a new token pending when it is absent from last-saved markdown", () => {
    expect(
      replaceWikiLinksInHtml("<p>[[WikiLinks E2E Nowhere]]</p>", [], "Saved.")
    ).toBe(
      '<p><a href="#" class="pending-wiki-link" data-wiki-title="WikiLinks E2E Nowhere">WikiLinks E2E Nowhere</a></p>'
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
      '<p><a href="#" class="dead-wiki-link" data-wiki-title="WikiLinks E2E Already Missing">WikiLinks E2E Already Missing</a></p>'
    )
  })

  it("keeps a wikiTitles hit live even when last-saved markdown is provided", () => {
    expect(
      replaceWikiLinksInHtml(
        "<p>[[MyNote]]</p>",
        [wikiTitleFromAuthoredToken("MyNote", 42)],
        "[[MyNote]]"
      )
    ).toBe(
      `<p><a href="${noteShowHref(42)}" class="donut-wiki-link" data-wiki-title="MyNote" data-note-id="42">MyNote</a></p>`
    )
  })

  it("turns a pending anchor dead once the token is in last-saved markdown", () => {
    expect(
      replaceWikiLinksInHtml(
        '<p><a href="#" class="pending-wiki-link" data-wiki-title="WikiLinks E2E Nowhere">WikiLinks E2E Nowhere</a></p>',
        [],
        "See [[WikiLinks E2E Nowhere]]."
      )
    ).toBe(
      '<p><a href="#" class="dead-wiki-link" data-wiki-title="WikiLinks E2E Nowhere">WikiLinks E2E Nowhere</a></p>'
    )
  })

  it("upgrades a last-saved pending anchor to live when wikiTitles resolve", () => {
    expect(
      replaceWikiLinksInHtml(
        '<p><a href="#" class="pending-wiki-link" data-wiki-title="MyNote">MyNote</a></p>',
        [wikiTitleFromAuthoredToken("MyNote", 42)],
        "[[MyNote]]"
      )
    ).toBe(
      `<p><a href="${noteShowHref(42)}" class="donut-wiki-link" data-wiki-title="MyNote" data-note-id="42">MyNote</a></p>`
    )
  })

  it("upgrades an in-flight pending anchor to live when wikiTitles resolve", () => {
    expect(
      replaceWikiLinksInHtml(
        '<p><a href="#" class="pending-wiki-link" data-wiki-title="MyNote">MyNote</a></p>',
        [wikiTitleFromAuthoredToken("MyNote", 42)],
        "Saved."
      )
    ).toBe(
      `<p><a href="${noteShowHref(42)}" class="donut-wiki-link" data-wiki-title="MyNote" data-note-id="42">MyNote</a></p>`
    )
  })
})
