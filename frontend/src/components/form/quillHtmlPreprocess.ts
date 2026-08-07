const isEmptyElement = (el: Element): boolean => el.textContent?.trim() === ""

/** Merge consecutive same-level headers left by browser normalization. */
export const mergeConsecutiveHeaders = (tempDiv: HTMLElement): void => {
  const headers = tempDiv.querySelectorAll("h1, h2, h3, h4, h5, h6")
  for (let i = 0; i < headers.length; i++) {
    const current = headers[i] as HTMLElement
    if (!current.parentNode) continue
    const next = current.nextElementSibling as HTMLElement | null
    if (next && next.tagName === current.tagName) {
      const prev = current.previousElementSibling
      if (prev && isEmptyElement(prev)) {
        current.innerHTML += next.innerHTML
        next.remove()
        i--
      }
    }
  }
}

const extractCodeBlockLines = (containerContent: string): string[] => {
  const blockRegex = /<div[^>]*class="ql-code-block"[^>]*>([\s\S]*?)<\/div>/g
  const lines: string[] = []
  let blockMatch
  while ((blockMatch = blockRegex.exec(containerContent)) !== null) {
    let content = blockMatch[1]!
      .replace(/&nbsp;/g, " ")
      .replace(/&lt;/g, "<")
      .replace(/&gt;/g, ">")
      .replace(/&amp;/g, "&")
    if (/^<br\s*\/?>$/i.test(content.trim())) {
      content = ""
    }
    lines.push(content)
  }
  return lines
}

/** Preserve Quill code-block text before DOM parsing loses leading spaces. */
export const preserveCodeBlockContent = (html: string): string => {
  let result = ""
  let lastIndex = 0
  const containerOpenRegex = /<div[^>]*class="ql-code-block-container"[^>]*>/g
  let match

  while ((match = containerOpenRegex.exec(html)) !== null) {
    result += html.substring(lastIndex, match.index)
    const openTag = match[0]
    const contentStart = match.index + openTag.length

    let depth = 1
    let pos = contentStart
    let containerEnd = html.length
    while (depth > 0 && pos < html.length) {
      const openPos = html.indexOf("<div", pos)
      const closePos = html.indexOf("</div>", pos)
      if (closePos === -1) break
      if (openPos !== -1 && openPos < closePos) {
        depth++
        pos = openPos + 4
      } else {
        depth--
        if (depth === 0) containerEnd = closePos
        pos = closePos + 6
      }
    }

    const containerContent = html.substring(contentStart, containerEnd)
    const lines = extractCodeBlockLines(containerContent)

    let newOpenTag = openTag
    if (lines.length > 0) {
      const escapedContent = lines.join("\n").replace(/"/g, "&quot;")
      newOpenTag = openTag.replace(
        `class="ql-code-block-container"`,
        `class="ql-code-block-container" data-preserved-content="${escapedContent}"`
      )
    }

    result += `${newOpenTag}${containerContent}</div>`
    lastIndex = containerEnd + 6
  }

  return result + html.substring(lastIndex)
}

/** Unwrap `<p>` tags inside table cells so turndown keeps table structure. */
export const normalizeTableCells = (tempDiv: HTMLElement): void => {
  const cells = tempDiv.querySelectorAll("th, td")
  cells.forEach((cell) => {
    const paragraphs = Array.from(cell.querySelectorAll("p"))
    paragraphs.forEach((p) => {
      const parent = p.parentNode
      if (parent) {
        while (p.firstChild) {
          parent.insertBefore(p.firstChild, p)
        }
        parent.removeChild(p)
      }
    })
    const childNodes = Array.from(cell.childNodes)
    while (
      childNodes.length > 0 &&
      childNodes[0]?.nodeType === Node.TEXT_NODE &&
      childNodes[0].textContent?.trim() === ""
    ) {
      cell.removeChild(childNodes[0]!)
      childNodes.shift()
    }
    while (
      childNodes.length > 0 &&
      childNodes[childNodes.length - 1]?.nodeType === Node.TEXT_NODE &&
      childNodes[childNodes.length - 1]?.textContent?.trim() === ""
    ) {
      cell.removeChild(childNodes[childNodes.length - 1]!)
      childNodes.pop()
    }
  })
}
