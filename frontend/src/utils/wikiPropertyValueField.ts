import type { WikiTitle } from "@generated/doughnut-backend-api"
import { noteShowHref } from "@/routes/noteShowLocation"

export function escapeHtmlForWikiPropertyValue(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
}

function escapeHtmlAttributeValue(s: string): string {
  return s.replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/</g, "&lt;")
}

/** `[[` / `]]` shown literally; title text escaped (same visible shape as plain wiki syntax). */
function wikiLinkBracketedInnerHtml(plainTitleInner: string): string {
  return `<span class="wiki-bracket">[[</span>${escapeHtmlForWikiPropertyValue(plainTitleInner)}<span class="wiki-bracket">]]</span>`
}

/** Valid wiki segment: non-empty after trim, no brackets or newlines inside (regex already constrains). */
function isValidPropertyWikiInner(rawBetweenBrackets: string): boolean {
  return rawBetweenBrackets.trim().length > 0
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
    map.set(w.linkText, w.noteId)
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

    const noteId = map.get(titleRaw) ?? map.get(titleRaw.trim())
    const innerHtml = wikiLinkBracketedInnerHtml(titleRaw)
    const attr = escapeHtmlAttributeValue(titleRaw)
    if (noteId !== undefined) {
      out += `<a href="${noteShowHref(noteId)}" class="doughnut-link" data-wiki-title="${attr}">${innerHtml}</a>`
    } else {
      out += `<a href="#" class="dead-link" data-wiki-title="${attr}">${innerHtml}</a>`
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
  const raw = anchor.textContent?.trim() ?? ""
  const closed = /^\[\[([^\[\]\r\n]*)\]\]$/.exec(raw)
  if (closed?.[1] !== undefined) return closed[1].trim()
  const open = /^\[\[([^\[\]\r\n]*)$/.exec(raw)
  if (open?.[1] !== undefined) return open[1].trim()
  return raw
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
        out += node.textContent ?? ""
        continue
      }
      out += node.textContent ?? ""
    } else if (node instanceof HTMLElement) {
      out += node.textContent ?? ""
    }
  }
  return out.replace(/\r?\n/g, "")
}
