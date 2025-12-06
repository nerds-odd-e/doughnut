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
    if (replacement === "<br>" || replacement === '<br class="softbreak">') {
      // Normalize <br class="softbreak"> to <br> in output
      return "<p><br></p>"
    }
    return replacement ? `\n\n${turndownService.turndown(replacement)}\n\n` : ""
  },
})

// Pre-process HTML to preserve code block content before DOM parsing
const preserveCodeBlockContent = (html: string): string => {
  // Extract content from ql-code-block divs directly from the HTML string
  // to preserve leading spaces that would be lost during DOM parsing
  const blockRegex = /<div[^>]*class="ql-code-block"[^>]*>([\s\S]*?)<\/div>/g
  const lines: string[] = []
  let blockMatch
  while ((blockMatch = blockRegex.exec(html)) !== null) {
    let content = blockMatch[1]
      .replace(/&nbsp;/g, " ")
      .replace(/&lt;/g, "<")
      .replace(/&gt;/g, ">")
      .replace(/&amp;/g, "&")
    // Convert <br> or <br/> to empty line
    if (/^<br\s*\/?>$/i.test(content.trim())) {
      content = ""
    }
    lines.push(content)
  }
  if (lines.length === 0) {
    return html
  }
  const preservedContent = lines.join("\n")
  const escapedContent = preservedContent.replace(/"/g, "&quot;")
  // Add data attribute to the container with preserved content
  return html.replace(
    /class="ql-code-block-container"/,
    `class="ql-code-block-container" data-preserved-content="${escapedContent}"`
  )
}

export default function htmlToMarkdown(html: string) {
  // Pre-process HTML to preserve code block content before DOM parsing
  const processedHtml = preserveCodeBlockContent(html)
  // Pre-process HTML to merge consecutive headers of the same level
  const tempDiv = document.createElement("div")
  tempDiv.innerHTML = processedHtml
  mergeConsecutiveHeaders(tempDiv)
  return turndownService.turndown(tempDiv.innerHTML)
}
