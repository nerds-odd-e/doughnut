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
  return turndownService.turndown(html)
}
