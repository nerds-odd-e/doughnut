import { describe, expect, it } from "vitest"
import {
  deadWikiLinkPayloadFromAnchor,
  escapeHtmlForWikiLinkDisplay,
  handleRichContentAnchorClick,
  markdownWikiTokenFromDeadWikiLinkPayload,
} from "@/utils/wikiLinkMarkup"

describe("wikiLinkMarkup utils", () => {
  it("handleRichContentAnchorClick emits dead link before checking href", () => {
    const anchor = document.createElement("a")
    anchor.className = "dead-wiki-link"
    anchor.setAttribute("data-wiki-title", "Ghost")
    anchor.textContent = "Ghost"
    let payload: { targetToken: string; displayText: string } | undefined
    handleRichContentAnchorClick(
      anchor,
      {
        onDeadWikiLink: (p) => {
          payload = p
        },
        navigateInApp: () => {
          throw new Error("should not navigate")
        },
      },
      { deadWikiLinksEnabled: true }
    )
    expect(payload).toEqual({ targetToken: "Ghost", displayText: "Ghost" })
  })

  it("handleRichContentAnchorClick navigates path markdown wiki links via data-note-id", () => {
    const anchor = document.createElement("a")
    anchor.className = "donut-wiki-link"
    anchor.setAttribute("href", "/Folder/Title.md")
    anchor.setAttribute("data-note-id", "42")
    anchor.textContent = "label"
    let navigated: string | undefined
    handleRichContentAnchorClick(
      anchor,
      {
        onDeadWikiLink: () => {
          throw new Error("should not treat as dead")
        },
        navigateInApp: (href) => {
          navigated = href
        },
      },
      { deadWikiLinksEnabled: true }
    )
    expect(navigated).toBe("/n42")
  })

  it("markdownWikiTokenFromDeadWikiLinkPayload matches simple and piped stored tokens", () => {
    expect(
      markdownWikiTokenFromDeadWikiLinkPayload({
        targetToken: "a",
        displayText: "a",
      })
    ).toBe("[[a]]")
    expect(
      markdownWikiTokenFromDeadWikiLinkPayload({
        targetToken: "Target",
        displayText: "label",
      })
    ).toBe("[[Target|label]]")
  })

  it("escapes HTML-sensitive characters for display pipeline", () => {
    expect(escapeHtmlForWikiLinkDisplay(`a<b>"c`)).toBe("a&lt;b&gt;&quot;c")
  })

  it("deadWikiLinkPayloadFromAnchor reads title from incomplete visible wiki text", () => {
    const a = document.createElement("a")
    a.className = "dead-wiki-link"
    a.textContent = "[[Eng"
    expect(deadWikiLinkPayloadFromAnchor(a).targetToken).toBe("Eng")
  })
})
