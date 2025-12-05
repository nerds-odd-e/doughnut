import { unified } from "unified"
import remarkParse from "remark-parse"
import remarkGfm from "remark-gfm"
import remarkRehype from "remark-rehype"
import rehypeStringify from "rehype-stringify"
import type { Root, List, ListItem, Paragraph } from "mdast"
import type { Element } from "hast"

export default function markdownToQuillHtml(
  markdown: string | undefined
): string {
  if (!markdown) return ""

  const convertHtmlList = (html: string): string => {
    return html
      .replace(/<ul>/g, "<ol>")
      .replace(/<\/ul>/g, "</ol>")
      .replace(/<li>/g, '<li data-list="bullet">')
  }

  const processor = unified()
    .use(remarkParse)
    .use(remarkGfm)
    .use(() => {
      return (tree: Root) => {
        const processNode = (
          node: any,
          indent: number = -1,
          depth: number = 0
        ): any => {
          if (node.type === "list") {
            const list = node as List
            const isOrdered = list.ordered === true
            const currentIndent = depth
            if (list.children) {
              list.children = list.children.map((item: ListItem) => {
                if (item.children) {
                  item.children = item.children.flatMap((child: any) => {
                    if (child.type === "list") {
                      const nestedList = processNode(child, indent, depth + 1)
                      return nestedList.children || []
                    } else if (child.type === "paragraph" && depth >= 0) {
                      return (child as Paragraph).children || []
                    }
                    return child
                  })
                }
                return {
                  ...item,
                  __indent: currentIndent,
                  __ordered: isOrdered,
                }
              })
            }
            return {
              ...list,
              ordered: true,
              __indent: currentIndent,
              __ordered: isOrdered,
            }
          }
          if (node.children) {
            return {
              ...node,
              children: node.children.map((child: any) =>
                processNode(child, indent, depth)
              ),
            }
          }
          return node
        }
        tree.children = tree.children.map((child: any) =>
          processNode(child, -1, 0)
        )
      }
    })
    .use(remarkRehype, { allowDangerousHtml: true })
    .use(() => {
      return (tree: any) => {
        const flattenOlElements = (node: any, baseIndent: number = 0): any => {
          if (
            node.type === "element" &&
            (node.tagName === "ol" || node.tagName === "ul")
          ) {
            const isOrderedFromTag = node.tagName === "ol"
            const flattened: any[] = []
            if (node.children) {
              node.children.forEach((child: any) => {
                if (child.type === "element" && child.tagName === "li") {
                  const li = child as Element
                  const existingAttrs = li.properties || {}
                  const isOrdered =
                    isOrderedFromTag || (child as any).__ordered === true
                  const listType = isOrdered ? "ordered" : "bullet"
                  li.properties = {
                    ...existingAttrs,
                    "data-list": listType,
                    ...(baseIndent > 0 && { class: `ql-indent-${baseIndent}` }),
                  }
                  flattened.push(child)
                  if (child.children) {
                    child.children.forEach((grandchild: any) => {
                      if (
                        grandchild.type === "element" &&
                        (grandchild.tagName === "ol" ||
                          grandchild.tagName === "ul")
                      ) {
                        const nestedIsOrdered = grandchild.tagName === "ol"
                        const nestedItems = flattenOlElements(
                          grandchild,
                          baseIndent + 1
                        )
                        if (nestedItems.children) {
                          nestedItems.children.forEach((nestedItem: any) => {
                            const nestedLi = nestedItem as Element
                            const nestedAttrs = nestedLi.properties || {}
                            const nestedListType = nestedIsOrdered
                              ? "ordered"
                              : "bullet"
                            nestedLi.properties = {
                              ...nestedAttrs,
                              "data-list": nestedListType,
                              ...(baseIndent + 1 > 0 && {
                                class: `ql-indent-${baseIndent + 1}`,
                              }),
                            }
                            flattened.push(nestedItem)
                          })
                        }
                      }
                    })
                  }
                }
              })
            }
            return { ...node, children: flattened }
          }
          if (node.children) {
            return {
              ...node,
              children: node.children.map((child: any) =>
                flattenOlElements(child, baseIndent)
              ),
            }
          }
          return node
        }

        const processElement = (node: any): any => {
          if (node.type === "element") {
            if (node.tagName === "ol" || node.tagName === "ul") {
              node.tagName = "ol"
              if (node.children) {
                node.children = node.children.map((child: any) => {
                  if (child.type === "element" && child.tagName === "li") {
                    const li = child as Element
                    const existingAttrs = li.properties || {}
                    const isOrdered = (child as any).__ordered || false
                    const listType = isOrdered ? "ordered" : "bullet"
                    const indent = (child as any).__indent || 0
                    li.properties = {
                      ...existingAttrs,
                      "data-list": listType,
                      ...(indent > 0 && { class: `ql-indent-${indent}` }),
                    }
                    if (child.children) {
                      child.children = child.children.map((grandchild: any) =>
                        processElement(grandchild)
                      )
                    }
                    return child
                  }
                  return processElement(child)
                })
              }
              return node
            }
            if (node.tagName === "li") {
              const li = node as Element
              const existingAttrs = li.properties || {}
              const isOrdered = (node as any).__ordered || false
              const listType = isOrdered ? "ordered" : "bullet"
              li.properties = {
                ...existingAttrs,
                "data-list": listType,
              }
              if (node.children) {
                node.children = node.children.map((child: any) =>
                  processElement(child)
                )
              }
              return node
            }
            if (node.tagName === "p") {
              const p = node as Element
              const indent = (p as any).__indent
              if (indent !== undefined && indent >= 0) {
                if (p.children && p.children.length === 1) {
                  return p.children[0]
                }
                return { type: "text", value: "" }
              }
            }
          }
          if (node.children) {
            return {
              ...node,
              children: node.children.map((child: any) =>
                processElement(child)
              ),
            }
          }
          return node
        }
        tree.children = tree.children.map((child: any) =>
          processElement(flattenOlElements(child, 0))
        )
      }
    })
    .use(rehypeStringify, { allowDangerousHtml: true })

  const result = processor.processSync(markdown).toString()

  return convertHtmlList(result.trim().replace(/>\s+</g, "><"))
}
