import TurndownService from "turndown"
import { gfm } from "turndown-plugin-gfm"
import {
  hrefLooksLikeConceptNotePath,
  pathnameLooksLikeInternalNoteShow,
} from "@/routes/noteShowLocation"
import {
  mergeConsecutiveHeaders,
  normalizeTableCells,
  preserveCodeBlockContent,
} from "@/components/form/quillHtmlPreprocess"
import {
  DEAD_WIKI_LINK_CLASS,
  DOUGHNUT_WIKI_LINK_CLASS,
} from "@/utils/wikiLinkDomMarkers"
import { wikiAnchorToMarkdownToken } from "@/utils/wikiLinkMarkup"

export const turndownService = new TurndownService({
  br: "<br>",
})

turndownService.use(gfm)

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

turndownService.addRule("quillCodeBlockContainer", {
  filter(node) {
    return (
      node.nodeName === "DIV" &&
      (node as HTMLElement).classList.contains("ql-code-block-container")
    )
  },
  replacement(_, node) {
    // Convert Quill code block container to markdown fenced code blocks
    // Use pre-extracted content from data attribute to preserve leading spaces
    const container = node as HTMLElement
    const preservedContent = container.getAttribute("data-preserved-content")
    if (preservedContent) {
      return `\n\n\`\`\`\n${preservedContent}\n\`\`\`\n\n`
    }
    // Fallback to extracting from DOM (may lose leading spaces)
    const codeBlocks = container.querySelectorAll(".ql-code-block")
    const lines = Array.from(codeBlocks).map((block) => {
      // Check if block contains only <br> tag
      if (
        block.innerHTML.trim() === "<br>" ||
        block.innerHTML.trim() === "<br/>"
      ) {
        return ""
      }
      return block.textContent || ""
    })
    const codeContent = lines.join("\n")
    return `\n\n\`\`\`\n${codeContent}\n\`\`\`\n\n`
  },
})

turndownService.addRule("pre", {
  filter: "pre",
  replacement(_, node) {
    // Convert <pre> tags to markdown fenced code blocks
    // Use textContent directly to avoid escaping special characters like underscores
    const preElement = node as HTMLElement
    const content = preElement.textContent || ""
    return `\n\n\`\`\`\n${content.trim()}\n\`\`\`\n\n`
  },
})

turndownService.addRule("p", {
  filter: "p",
  replacement(_, node: Node) {
    const replacement = (node as HTMLElement).innerHTML
    return replacement ? `\n\n${turndownService.turndown(replacement)}\n\n` : ""
  },
})

// Helper to check if an element has text nodes containing HTML tag patterns
const hasTextNodesWithHtmlTags = (element: HTMLElement): boolean => {
  const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT)
  let node: Node | null
  while ((node = walker.nextNode())) {
    if (/<[a-zA-Z][a-zA-Z0-9]*\s*\/?>/.test(node.textContent || "")) {
      return true
    }
  }
  return false
}

// Helper to escape HTML tags in text nodes (they were originally escaped entities)
const escapeHtmlTagsInTextNodes = (element: HTMLElement): string => {
  let result = ""
  for (const child of Array.from(element.childNodes)) {
    if (child.nodeType === Node.TEXT_NODE) {
      const text = child.textContent || ""
      result += text.replace(
        /<([a-zA-Z][a-zA-Z0-9]*)(\s*\/?)>/g,
        (_match, tagName, selfClose) => `\\<${tagName}${selfClose}\\>`
      )
    } else if (child.nodeType === Node.ELEMENT_NODE) {
      const childElement = child as HTMLElement
      if (hasTextNodesWithHtmlTags(childElement)) {
        // Recursively process child elements that have HTML tags in text nodes
        result += escapeHtmlTagsInTextNodes(childElement)
      } else {
        result += turndownService.turndown(childElement.outerHTML)
      }
    }
  }
  return result
}

// Custom rule for spans that contain text nodes with HTML tag patterns (originally escaped entities)
turndownService.addRule("spanWithEscapedEntities", {
  filter(node) {
    if (node.nodeName !== "SPAN") return false
    return hasTextNodesWithHtmlTags(node as HTMLElement)
  },
  replacement(_content, node) {
    return escapeHtmlTagsInTextNodes(node as HTMLElement)
  },
})

// Custom rule for headers that contain text nodes with HTML tag patterns
turndownService.addRule("headerWithEscapedEntities", {
  filter(node) {
    if (!/^H[1-6]$/.test(node.nodeName)) return false
    return hasTextNodesWithHtmlTags(node as HTMLElement)
  },
  replacement(_content, node) {
    const level = parseInt(node.nodeName[1]!, 10)
    const prefix = `${"#".repeat(level)} `
    return `\n\n${prefix}${escapeHtmlTagsInTextNodes(node as HTMLElement)}\n\n`
  },
})

// Custom rule for bold/strong that contain text nodes with HTML tag patterns
turndownService.addRule("boldWithEscapedEntities", {
  filter(node) {
    if (node.nodeName !== "B" && node.nodeName !== "STRONG") return false
    return hasTextNodesWithHtmlTags(node as HTMLElement)
  },
  replacement(_content, node) {
    return `**${escapeHtmlTagsInTextNodes(node as HTMLElement)}**`
  },
})

// Custom rule for italic/em that contain text nodes with HTML tag patterns
turndownService.addRule("italicWithEscapedEntities", {
  filter(node) {
    if (node.nodeName !== "I" && node.nodeName !== "EM") return false
    return hasTextNodesWithHtmlTags(node as HTMLElement)
  },
  replacement(_content, node) {
    return `*${escapeHtmlTagsInTextNodes(node as HTMLElement)}*`
  },
})

turndownService.addRule("doughnutWikiNoteLink", {
  filter(node) {
    if (node.nodeName !== "A") return false
    return (node as HTMLElement).classList.contains(DOUGHNUT_WIKI_LINK_CLASS)
  },
  replacement(_content, node) {
    const el = node as HTMLAnchorElement
    const href = el.getAttribute("href") ?? ""
    if (hrefLooksLikeConceptNotePath(href)) {
      const display =
        el.getAttribute("data-wiki-display") || el.textContent?.trim() || ""
      return `[${display}](${href})`
    }
    return wikiAnchorToMarkdownToken(el)
  },
})

turndownService.addRule("doughnutDeadWikiLink", {
  filter(node) {
    if (node.nodeName !== "A") return false
    return (node as HTMLElement).classList.contains(DEAD_WIKI_LINK_CLASS)
  },
  replacement(_content, node) {
    return wikiAnchorToMarkdownToken(node as HTMLAnchorElement)
  },
})

/** Pasted HTML often has plain note-show hrefs without doughnut-wiki-link class. */
function hrefIsInternalNoteShow(href: string | null): boolean {
  if (!href?.trim()) return false
  try {
    const pathname = new URL(href, "https://example.invalid").pathname
    return pathnameLooksLikeInternalNoteShow(pathname)
  } catch {
    return false
  }
}

turndownService.addRule("doughnutNoteShowHrefWikiLink", {
  filter(node) {
    if (node.nodeName !== "A") return false
    const el = node as HTMLAnchorElement
    if (el.classList.contains(DOUGHNUT_WIKI_LINK_CLASS)) {
      return false
    }
    return hrefIsInternalNoteShow(el.getAttribute("href"))
  },
  replacement(_content, node) {
    const text = (node as HTMLElement).textContent?.trim() ?? ""
    return `[[${text}]]`
  },
})

export default function htmlToMarkdown(html: string) {
  // Pre-process HTML to preserve code block content before DOM parsing
  const processedHtml = preserveCodeBlockContent(html)
  // Parse HTML and merge consecutive headers of the same level
  const tempDiv = document.createElement("div")
  tempDiv.innerHTML = processedHtml
  // Normalize table cells by removing <p> tags inside them
  normalizeTableCells(tempDiv)
  mergeConsecutiveHeaders(tempDiv)
  const markdown = turndownService.turndown(tempDiv.innerHTML)
  return markdown.replace(/\\\[\\\[((?:[^\\\[\]]|\\\[)+?)\\\]\\\]/g, "[[$1]]")
}
