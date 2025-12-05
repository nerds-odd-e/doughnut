import { unified } from "unified"
import rehypeParse from "rehype-parse"
import rehypeRemark from "rehype-remark"
import remarkStringify from "remark-stringify"

export default function htmlToMarkdown(html: string) {
  const markdownParts: string[] = []

  const listRegex = /<ol[^>]*>([\s\S]*?)<\/ol>/gi
  let listMatch
  const listItems: string[] = []

  while ((listMatch = listRegex.exec(html)) !== null) {
    const listContent = listMatch[1]
    const liRegex = /<li[^>]*>([\s\S]*?)<\/li>/gi
    let liMatch

    while ((liMatch = liRegex.exec(listContent)) !== null) {
      const fullLi = liMatch[0]
      let content = liMatch[1]

      const dataListMatch = fullLi.match(/data-list=['"]([^'"]+)['"]/)
      const listType = dataListMatch ? dataListMatch[1] : "bullet"

      const classMatch = fullLi.match(/class=['"]([^'"]*)['"]/)
      const className = classMatch ? classMatch[1] : ""

      const tempDiv = new DOMParser().parseFromString(
        `<div>${content}</div>`,
        "text/html"
      )
      content =
        tempDiv.body.textContent || content.replace(/<[^>]*>/g, "").trim()

      const indentMatch = className.match(/ql-indent-(\d+)/)
      const indentLevel = indentMatch ? parseInt(indentMatch[1]!, 10) : 0

      const indent = "  ".repeat(indentLevel)
      const bullet = listType === "ordered" ? "1." : "*"

      listItems.push(`${indent}${bullet} ${content}`)
    }
  }

  if (listItems.length > 0) {
    markdownParts.push(listItems.join("\n"))
  }

  const nonListHtml = html.replace(/<ol[^>]*>[\s\S]*?<\/ol>/gi, "")
  if (nonListHtml.trim()) {
    const processor = unified()
      .use(rehypeParse, { fragment: true })
      .use(() => {
        return (tree: any) => {
          const processNode = (node: any): any => {
            if (node.type === "element" && node.tagName === "h1") {
              const extractText = (n: any): string => {
                if (n.type === "text") return n.value || ""
                if (n.children) {
                  return n.children.map((c: any) => extractText(c)).join("")
                }
                return ""
              }
              const content = extractText(node).trim()
              const underline = "=".repeat(content.length)
              return { type: "text", value: `${content}\n${underline}` }
            }
            if (node.type === "element" && node.tagName === "p") {
              const p = node as any
              if (p.children && p.children.length === 1) {
                const firstChild = p.children[0]
                if (
                  firstChild?.type === "element" &&
                  firstChild.tagName === "br"
                ) {
                  return { type: "text", value: "<p><br></p>" }
                }
              }
            }
            if (node.children) {
              return {
                ...node,
                children: node.children.map((child: any) => processNode(child)),
              }
            }
            return node
          }
          tree.children = tree.children.map((child: any) => processNode(child))
        }
      })
      .use(rehypeRemark)
      .use(remarkStringify, {
        bullet: "*",
        bulletOther: "*",
        bulletOrdered: "1.",
        emphasis: "_",
      })
    const nonListMarkdown = processor.processSync(nonListHtml).toString().trim()
    if (nonListMarkdown) {
      if (listItems.length > 0) {
        markdownParts.unshift(nonListMarkdown)
      } else {
        markdownParts.push(nonListMarkdown)
      }
    }
  } else if (listItems.length === 0) {
    const processor = unified()
      .use(rehypeParse, { fragment: true })
      .use(() => {
        return (tree: any) => {
          const processNode = (node: any): any => {
            if (node.type === "element" && node.tagName === "h1") {
              const extractText = (n: any): string => {
                if (n.type === "text") return n.value || ""
                if (n.children) {
                  return n.children.map((c: any) => extractText(c)).join("")
                }
                return ""
              }
              const content = extractText(node).trim()
              const underline = "=".repeat(content.length)
              return { type: "text", value: `${content}\n${underline}` }
            }
            if (node.type === "element" && node.tagName === "p") {
              const p = node as any
              if (p.children && p.children.length === 1) {
                const firstChild = p.children[0]
                if (
                  firstChild?.type === "element" &&
                  firstChild.tagName === "br"
                ) {
                  return { type: "text", value: "<p><br></p>" }
                }
              }
            }
            if (node.children) {
              return {
                ...node,
                children: node.children.map((child: any) => processNode(child)),
              }
            }
            return node
          }
          tree.children = tree.children.map((child: any) => processNode(child))
        }
      })
      .use(rehypeRemark)
      .use(remarkStringify, {
        bullet: "*",
        bulletOther: "*",
        bulletOrdered: "1.",
        emphasis: "_",
      })
    const result = processor.processSync(html).toString().trim()
    return result
  }

  return markdownParts.join("").trim()
}
