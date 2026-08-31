import { describe, expect, it } from "vitest"
import type { RouteLocationRaw } from "vue-router"
import type { WikiLink } from "@generated/donut-backend-api"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import {
  deadWikiLinkPayloadFromAnchor,
  escapeHtmlForWikiLinkDisplay,
  handleRichContentAnchorClick,
  markdownWikiTokenFromDeadWikiLinkPayload,
  wikiLinkFromAuthoredToken,
  wikiLinkNoteIdLookup,
} from "@/utils/wikiLinkMarkup"

describe("wikiLinkMarkup utils", () => {
  it("wikiLinkNoteIdLookup maps only RESOLVED destination ids", () => {
    const ambiguous: WikiLink = {
      authoredLink: "Shared",
      portablePath: "Shared",
      displayText: "Shared",
      resolution: "AMBIGUOUS",
    }
    const map = wikiLinkNoteIdLookup([
      wikiLinkFromAuthoredToken("Live", 7),
      ambiguous,
    ])
    expect(map.get("Live")).toBe(7)
    expect(map.has("Shared")).toBe(false)
  })

  it("handleRichContentAnchorClick emits dead link before checking href", () => {
    const anchor = document.createElement("a")
    anchor.className = "dead-wiki-link"
    anchor.setAttribute("data-portable-path", "Ghost")
    anchor.textContent = "Ghost"
    let payload: { portablePath: string; displayText: string } | undefined
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
    expect(payload).toEqual({ portablePath: "Ghost", displayText: "Ghost" })
  })

  it("handleRichContentAnchorClick carries AMBIGUOUS from the anchor onto the payload", () => {
    const anchor = document.createElement("a")
    anchor.className = "dead-wiki-link"
    anchor.setAttribute("data-portable-path", "Shared")
    anchor.setAttribute("data-resolution", "AMBIGUOUS")
    anchor.textContent = "Shared"
    let payload:
      | { portablePath: string; displayText: string; resolution?: string }
      | undefined
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
    expect(payload).toEqual({
      portablePath: "Shared",
      displayText: "Shared",
      resolution: "AMBIGUOUS",
    })
  })

  it("handleRichContentAnchorClick navigates path markdown wiki links via data-note-id", () => {
    const anchor = document.createElement("a")
    anchor.className = "donut-wiki-link"
    anchor.setAttribute("href", "/Folder/Title.md")
    anchor.setAttribute("data-note-id", "42")
    anchor.textContent = "label"
    let navigated: RouteLocationRaw | undefined
    handleRichContentAnchorClick(
      anchor,
      {
        onDeadWikiLink: () => {
          throw new Error("should not treat as dead")
        },
        navigateInApp: (to) => {
          navigated = to
        },
      },
      { deadWikiLinksEnabled: true }
    )
    expect(navigated).toEqual(noteShowLocation(42))
  })

  it("handleRichContentAnchorClick pushes noteProperty when the authored target has a property suffix", () => {
    const anchor = document.createElement("a")
    anchor.className = "donut-wiki-link"
    anchor.setAttribute("data-portable-path", "Moon#prop:a%20part%20of")
    anchor.setAttribute("data-note-id", "42")
    let navigated: RouteLocationRaw | undefined
    handleRichContentAnchorClick(
      anchor,
      {
        onDeadWikiLink: () => {
          throw new Error("should not treat as dead")
        },
        navigateInApp: (to) => {
          navigated = to
        },
      },
      { deadWikiLinksEnabled: true }
    )
    expect(navigated).toEqual(notePropertyLocation(42, "a part of"))
  })

  it.each([
    { href: "#", kind: "hash" },
    { href: "/Folder/Title.md", kind: "portable-path" },
    {
      href: "/Solar/Moon.md#prop:a%20part%20of",
      kind: "portable-path-with-prop-fragment",
    },
  ])(
    "handleRichContentAnchorClick does not navigate leftover $kind hrefs",
    ({ href }) => {
      const anchor = document.createElement("a")
      anchor.setAttribute("href", href)
      handleRichContentAnchorClick(
        anchor,
        {
          onDeadWikiLink: () => {
            throw new Error("should not treat as dead")
          },
          navigateInApp: () => {
            throw new Error("should not navigate")
          },
        },
        { deadWikiLinksEnabled: true }
      )
    }
  )

  it("markdownWikiTokenFromDeadWikiLinkPayload matches simple and piped stored tokens", () => {
    expect(
      markdownWikiTokenFromDeadWikiLinkPayload({
        portablePath: "a",
        displayText: "a",
      })
    ).toBe("[[a]]")
    expect(
      markdownWikiTokenFromDeadWikiLinkPayload({
        portablePath: "Target",
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
    expect(deadWikiLinkPayloadFromAnchor(a).portablePath).toBe("Eng")
  })
})
