import type { WikiTitle } from "@generated/doughnut-backend-api"
import { noteShowHref } from "@/routes/noteShowLocation"

/** Builds API-shaped {@link WikiTitle} for tests and local fixtures from markdown inner + note id. */
export function wikiTitleFromInnerAndNoteId(
  inner: string,
  noteId: number
): WikiTitle {
  const { target, display } = splitWikiLinkInner(inner)
  return { linkText: inner, targetToken: target, displayText: display, noteId }
}

/** Normalized target, display label, and full inner for a wiki title from the note realm. */
export function wikiTitleParts(w: WikiTitle): {
  target: string
  display: string
  inner: string
} {
  return { target: w.targetToken, display: w.displayText, inner: w.linkText }
}

export function escapeHtmlForWikiPropertyValue(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
}

export function escapeHtmlAttributeValue(s: string): string {
  return s.replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/</g, "&lt;")
}

/** `[[` / `]]` shown literally; title text escaped (same visible shape as plain wiki syntax). */
export function wikiLinkBracketedInnerHtml(plainTitleInner: string): string {
  return `<span class="wiki-bracket">[[</span>${escapeHtmlForWikiPropertyValue(plainTitleInner)}<span class="wiki-bracket">]]</span>`
}

/** Valid wiki segment: non-empty after trim, no brackets or newlines inside (regex already constrains). */
export function isValidPropertyWikiInner(rawBetweenBrackets: string): boolean {
  return rawBetweenBrackets.trim().length > 0
}

/** Splits inner wiki text on the first `|`; empty right-hand side is treated as no pipe. */
export function splitWikiLinkInner(rawBetweenBrackets: string): {
  target: string
  display: string
} {
  const i = rawBetweenBrackets.indexOf("|")
  if (i === -1) {
    return { target: rawBetweenBrackets, display: rawBetweenBrackets }
  }
  const target = rawBetweenBrackets.slice(0, i)
  const display = rawBetweenBrackets.slice(i + 1)
  if (display.trim().length === 0) {
    return { target, display: target }
  }
  return { target, display }
}

/**
 * Renders a YAML property scalar with clickable wiki links. Only well-formed `[[title]]` segments
 * (non-empty title, no `[`/`]`/newlines inside) become links; everything else stays plain text.
 */
export function propertyValuePlainToDisplayHtml(
  plain: string,
  wikiTitles: WikiTitle[]
): string {
  const map = new Map<string, number>()
  for (const w of wikiTitles) {
    const { target } = wikiTitleParts(w)
    map.set(target.trim(), w.noteId)
    map.set(w.linkText.trim(), w.noteId)
  }

  const re = /\[\[([^\[\]\r\n]*)\]\]/g
  let out = ""
  let lastIndex = 0
  let m: RegExpExecArray | null
  while ((m = re.exec(plain)) !== null) {
    const fullMatch = m[0]
    const titleRaw = m[1] ?? ""
    const start = m.index

    out += escapeHtmlForWikiPropertyValue(plain.slice(lastIndex, start))
    lastIndex = start + fullMatch.length

    if (!isValidPropertyWikiInner(titleRaw)) {
      out += escapeHtmlForWikiPropertyValue(fullMatch)
      continue
    }

    const { target, display } = splitWikiLinkInner(titleRaw)
    const noteId = map.get(titleRaw.trim()) ?? map.get(target.trim())
    const innerHtml = wikiLinkBracketedInnerHtml(display)
    const attrTarget = escapeHtmlAttributeValue(target)
    const displayAttr =
      display !== target
        ? ` data-wiki-display="${escapeHtmlAttributeValue(display)}"`
        : ""
    if (noteId !== undefined) {
      out += `<a href="${noteShowHref(noteId)}" class="doughnut-link" data-wiki-title="${attrTarget}"${displayAttr}>${innerHtml}</a>`
    } else {
      out += `<a href="#" class="dead-link" data-wiki-title="${attrTarget}"${displayAttr}>${innerHtml}</a>`
    }
  }
  out += escapeHtmlForWikiPropertyValue(plain.slice(lastIndex))
  return out
}

/**
 * Title for dead-link create flow: prefer well-formed `[[title]]` inside visible text;
 * otherwise text after `[[`, or full trimmed text (matches what the user actually typed).
 */
export function deadLinkCreateTitleFromAnchor(anchor: HTMLElement): string {
  return deadLinkPayloadFromAnchor(anchor).targetToken
}

/** Dead-link click payload containing the target token and visible display text. */
export type DeadLinkPayload = { targetToken: string; displayText: string }

/** Extracts target token and display text from a dead-link anchor element. */
export function deadLinkPayloadFromAnchor(
  anchor: HTMLElement
): DeadLinkPayload {
  const raw = anchor.textContent?.trim() ?? ""
  let targetToken: string
  const fromAttr = anchor.getAttribute("data-wiki-title")
  if (fromAttr !== null && fromAttr !== "") {
    targetToken = fromAttr
  } else {
    const closed = /^\[\[([^\[\]\r\n]*)\]\]$/.exec(raw)
    if (closed?.[1] !== undefined) {
      targetToken = closed[1].trim()
    } else {
      const open = /^\[\[([^\[\]\r\n]*)$/.exec(raw)
      targetToken = open?.[1]?.trim() ?? raw
    }
  }

  const displayAttr = anchor.getAttribute("data-wiki-display")
  if (displayAttr !== null && displayAttr !== "") {
    return { targetToken, displayText: displayAttr }
  }
  return { targetToken, displayText: targetToken }
}

/** Markdown token for a wiki anchor (dead or live) from DOM; prefers `data-wiki-title` / bracketed display. */
export function wikiAnchorToMarkdownToken(anchor: HTMLAnchorElement): string {
  const raw = anchor.textContent?.trim() ?? ""
  const target = anchor.getAttribute("data-wiki-title")
  if (target === null || target === "") {
    const bracketed = /^\[\[([\s\S]*)\]\]$/.exec(raw)
    if (bracketed?.[1] !== undefined) {
      return `[[${bracketed[1]}]]`
    }
    return `[[${raw}]]`
  }

  const fromDisplayAttr = anchor.getAttribute("data-wiki-display")
  const innerM = /^\[\[([\s\S]*)\]\]$/.exec(raw)

  if (fromDisplayAttr !== null && fromDisplayAttr !== "") {
    const displayPart = fromDisplayAttr
    if (displayPart === target) {
      return `[[${target}]]`
    }
    return `[[${target}|${displayPart}]]`
  }

  if (innerM !== null) {
    const visibleInner = innerM[1]!
    if (visibleInner === target) {
      return `[[${target}]]`
    }
    return `[[${target}|${visibleInner}]]`
  }

  if (raw.startsWith("[[") && !raw.endsWith("]]")) {
    return raw
  }

  if (raw === target) {
    return `[[${target}]]`
  }
  return `[[${target}|${raw}]]`
}

/** Serializes the editor root (top-level nodes) back to a plain scalar. Wiki anchors use visible text only (so in-place edits are saved). */
export function serializeWikiPropertyValueFieldRoot(el: HTMLElement): string {
  let out = ""
  for (const node of el.childNodes) {
    if (node.nodeType === Node.TEXT_NODE) {
      out += node.textContent ?? ""
    } else if (node instanceof HTMLBRElement) {
      continue
    } else if (node instanceof HTMLAnchorElement) {
      if (
        node.classList.contains("doughnut-link") ||
        node.classList.contains("dead-link")
      ) {
        out += wikiAnchorToMarkdownToken(node)
        continue
      }
      out += node.textContent ?? ""
    } else if (node instanceof HTMLElement) {
      out += node.textContent ?? ""
    }
  }
  return out.replace(/\r?\n/g, "")
}
