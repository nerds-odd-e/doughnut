import TurndownService from "turndown"

export const turndownService = new TurndownService({
  br: "<br>",
})

turndownService.addRule("quillListItem", {
  filter(node) {
    return node.nodeName === "LI" && node.getAttribute("data-list") != null
  },
  replacement(content, node, options) {
    const listType = (node as HTMLElement).getAttribute("data-list")
    let bullet: string | undefined = options.bulletListMarker
    if (listType === "ordered") {
      bullet = "1."
    }
    const className = (node as HTMLElement).getAttribute("class") || ""
    const indentMatch = className.match(/ql-indent-(\d+)/)
    const indentLevel = indentMatch ? parseInt(indentMatch[1]!, 10) : 0
    const indent = "  ".repeat(indentLevel)
    return `\n${indent}${bullet} ${content.trim()}`
  },
})

const isEmptyElement = (el: Element): boolean => {
  // Check if element has no meaningful text content
  return el.textContent?.trim() === ""
}

const mergeConsecutiveHeaders = (tempDiv: HTMLElement): void => {
  // Merge consecutive headers of the same level, but only if there's evidence
  // they came from the same block (e.g., empty element remnants from browser normalization)
  const headers = tempDiv.querySelectorAll("h1, h2, h3, h4, h5, h6")
  for (let i = 0; i < headers.length; i++) {
    const current = headers[i] as HTMLElement
    if (!current.parentNode) continue // Already removed
    const next = current.nextElementSibling as HTMLElement | null
    if (next && next.tagName === current.tagName) {
      // Only merge if there's an empty element before the first header
      // (evidence of browser normalization pulling headers out of a wrapper)
      const prev = current.previousElementSibling
      if (prev && isEmptyElement(prev)) {
        current.innerHTML += next.innerHTML
        next.remove()
        i-- // Check again for more consecutive headers
      }
    }
  }
}

turndownService.addRule("p", {
  filter: "p",
  replacement(_, node: Node) {
    const replacement = (node as HTMLElement).innerHTML
    if (replacement === "<br>") {
      return (node as HTMLElement).outerHTML
    }
    return replacement ? `\n\n${turndownService.turndown(replacement)}\n\n` : ""
  },
})

export default function htmlToMarkdown(html: string) {
  // Pre-process HTML to merge consecutive headers of the same level
  const tempDiv = document.createElement("div")
  tempDiv.innerHTML = html
  mergeConsecutiveHeaders(tempDiv)
  return turndownService.turndown(tempDiv.innerHTML)
}
