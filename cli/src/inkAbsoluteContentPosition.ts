import type { DOMElement } from 'ink'
import Yoga from 'yoga-layout'

function getAbsoluteBorderPosition(
  node: DOMElement
): { x: number; y: number } | undefined {
  let currentNode: DOMElement | undefined = node
  let x = 0
  let y = 0
  while (currentNode?.parentNode) {
    if (!currentNode.yogaNode) {
      return undefined
    }
    x += currentNode.yogaNode.getComputedLeft()
    y += currentNode.yogaNode.getComputedTop()
    currentNode = currentNode.parentNode
  }
  return { x, y }
}

/** Ink-internal layout convention: terminal cells from output origin. */
export function getAbsoluteContentPosition(
  node: DOMElement | null | undefined
): { x: number; y: number } | undefined {
  if (!node) return undefined
  const borderPosition = getAbsoluteBorderPosition(node)
  if (!(borderPosition && node.yogaNode)) {
    return undefined
  }
  return {
    x:
      borderPosition.x +
      node.yogaNode.getComputedBorder(Yoga.EDGE_LEFT) +
      node.yogaNode.getComputedPadding(Yoga.EDGE_LEFT),
    y:
      borderPosition.y +
      node.yogaNode.getComputedBorder(Yoga.EDGE_TOP) +
      node.yogaNode.getComputedPadding(Yoga.EDGE_TOP),
  }
}
