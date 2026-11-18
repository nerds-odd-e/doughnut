import { computeLineDiff } from "./lineDiff"

interface MarkedText {
  type: "same" | "removed" | "added"
  text: string
}

export type ChangedLine =
  | MarkedText
  | { type: "replaced"; segments: MarkedText[] }

// A shorter run of common characters is a chance match: keeping it would
// cut one changed word into many marks.
const SHORTEST_COMMON_RUN = 3

const longestCommonRun = (from: string[], to: string[]) => {
  let longest = { from: 0, to: 0, length: 0 }
  let previous: number[] = []
  for (let i = 0; i < from.length; i++) {
    const lengths: number[] = []
    for (let j = 0; j < to.length; j++) {
      if (from[i] !== to[j]) continue
      const length = (previous[j - 1] ?? 0) + 1
      lengths[j] = length
      if (length > longest.length) {
        longest = { from: i - length + 1, to: j - length + 1, length }
      }
    }
    previous = lengths
  }
  return longest
}

const commonEnds = (from: string[], to: string[]) => {
  const shorter = Math.min(from.length, to.length)
  let prefix = 0
  while (prefix < shorter && from[prefix] === to[prefix]) prefix++
  let suffix = 0
  while (
    suffix < shorter - prefix &&
    from[from.length - 1 - suffix] === to[to.length - 1 - suffix]
  ) {
    suffix++
  }
  return { prefix, suffix }
}

// The common start and end of the two lines are found first, so a small
// edit in a long line costs about the size of the edit.
const commonRun = (from: string[], to: string[]) => {
  const { prefix, suffix } = commonEnds(from, to)
  if (prefix >= SHORTEST_COMMON_RUN) return { from: 0, to: 0, length: prefix }
  if (suffix >= SHORTEST_COMMON_RUN) {
    return {
      from: from.length - suffix,
      to: to.length - suffix,
      length: suffix,
    }
  }
  return longestCommonRun(from, to)
}

const changedCharacters = (from: string[], to: string[]): MarkedText[] => {
  const common = commonRun(from, to)
  if (common.length < SHORTEST_COMMON_RUN) {
    const changed: MarkedText[] = [
      { type: "removed", text: from.join("") },
      { type: "added", text: to.join("") },
    ]
    return changed.filter((segment) => segment.text)
  }
  const fromEnd = common.from + common.length
  return [
    ...changedCharacters(from.slice(0, common.from), to.slice(0, common.to)),
    { type: "same", text: from.slice(common.from, fromEnd).join("") },
    ...changedCharacters(
      from.slice(fromEnd),
      to.slice(common.to + common.length)
    ),
  ]
}

const changedBlock = (removed: string[], added: string[]): ChangedLine[] => {
  const lines: ChangedLine[] = []
  for (let i = 0; i < Math.max(removed.length, added.length); i++) {
    const from = removed[i]
    const to = added[i]
    const segments =
      from === undefined || to === undefined
        ? []
        : changedCharacters(Array.from(from), Array.from(to))
    if (segments.some((segment) => segment.type === "same")) {
      lines.push({ type: "replaced", segments })
    } else {
      if (from !== undefined) lines.push({ type: "removed", text: from })
      if (to !== undefined) lines.push({ type: "added", text: to })
    }
  }
  return lines
}

export const buildChangedLines = (from: string, to: string): ChangedLine[] => {
  const { currentLines: toLines, oldLines: fromLines } = computeLineDiff(
    to,
    from
  )
  const result: ChangedLine[] = []
  let fromAt = 0
  let toAt = 0
  while (fromAt < fromLines.length || toAt < toLines.length) {
    const removed: string[] = []
    const added: string[] = []
    while (fromLines[fromAt]?.type === "removed") {
      removed.push(fromLines[fromAt++]!.text)
    }
    while (toLines[toAt]?.type === "added") added.push(toLines[toAt++]!.text)
    result.push(...changedBlock(removed, added))
    if (toAt < toLines.length) {
      result.push({ type: "same", text: toLines[toAt++]!.text })
      fromAt++
    }
  }
  return result
}
