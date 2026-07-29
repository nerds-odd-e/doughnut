/** Unchanged lines kept on each side of a change, as `git diff` does. */
const CONTEXT_LINES = 3

export type DiffLineKind = 'context' | 'removed' | 'added'

export type DiffLine = {
  readonly kind: DiffLineKind
  readonly text: string
}

export type DiffHunk = {
  /**
   * The line the hunk starts at, 1-based, or undefined when the whole diff is
   * one hunk and a heading would say nothing.
   */
  readonly header: number | undefined
  readonly lines: readonly DiffLine[]
}

/**
 * Longest common subsequence of two line arrays, as the length table of the
 * classic dynamic program. `table[i][j]` is the LCS length of `a[i..]`/`b[j..]`.
 */
function lcsLengths(a: readonly string[], b: readonly string[]): number[][] {
  const table: number[][] = Array.from({ length: a.length + 1 }, () =>
    new Array<number>(b.length + 1).fill(0)
  )
  for (let i = a.length - 1; i >= 0; i--) {
    for (let j = b.length - 1; j >= 0; j--) {
      table[i]![j] =
        a[i] === b[j]
          ? table[i + 1]![j + 1]! + 1
          : Math.max(table[i + 1]![j]!, table[i]![j + 1]!)
    }
  }
  return table
}

type WalkedLine = DiffLine & {
  /** 1-based line number on the removed side, for hunk headers. */
  readonly beforeLine: number
}

function walk(
  before: readonly string[],
  after: readonly string[],
  firstBeforeLine: number
): WalkedLine[] {
  const table = lcsLengths(before, after)
  const walked: WalkedLine[] = []
  let i = 0
  let j = 0
  const beforeLine = () => firstBeforeLine + i
  while (i < before.length && j < after.length) {
    if (before[i] === after[j]) {
      walked.push({
        kind: 'context',
        text: before[i]!,
        beforeLine: beforeLine(),
      })
      i++
      j++
    } else if (table[i + 1]![j]! >= table[i]![j + 1]!) {
      walked.push({
        kind: 'removed',
        text: before[i]!,
        beforeLine: beforeLine(),
      })
      i++
    } else {
      walked.push({ kind: 'added', text: after[j]!, beforeLine: beforeLine() })
      j++
    }
  }
  while (i < before.length) {
    walked.push({ kind: 'removed', text: before[i]!, beforeLine: beforeLine() })
    i++
  }
  while (j < after.length) {
    walked.push({ kind: 'added', text: after[j]!, beforeLine: beforeLine() })
    j++
  }
  return walked
}

type TrimmedSides = {
  readonly before: readonly string[]
  readonly after: readonly string[]
  /** 1-based line the kept region starts at on the removed side. */
  readonly firstBeforeLine: number
}

/**
 * Narrow both sides to the region a diff can print, by dropping the unchanged
 * head and tail they share beyond the context lines.
 *
 * The comparison below costs the product of the two lengths in time and memory,
 * so a note of many lines with one line changed would otherwise pay for its
 * whole length. Only lines past the context are dropped, so every hunk still
 * prints the unchanged lines around its change, and the diff is the same one
 * comparing the sides whole would produce.
 */
function trimUnchangedEdges(
  before: readonly string[],
  after: readonly string[]
): TrimmedSides {
  const shorter = Math.min(before.length, after.length)
  let prefix = 0
  while (prefix < shorter && before[prefix] === after[prefix]) prefix++
  let suffix = 0
  while (
    suffix < shorter - prefix &&
    before[before.length - 1 - suffix] === after[after.length - 1 - suffix]
  ) {
    suffix++
  }

  const head = Math.max(0, prefix - CONTEXT_LINES)
  const tail = Math.max(0, suffix - CONTEXT_LINES)
  return {
    before: before.slice(head, before.length - tail),
    after: after.slice(head, after.length - tail),
    firstBeforeLine: head + 1,
  }
}

/** Index ranges of `walked` to keep: each change plus its surrounding context. */
function keptRanges(
  walked: readonly WalkedLine[]
): { from: number; to: number }[] {
  const ranges: { from: number; to: number }[] = []
  walked.forEach((line, index) => {
    if (line.kind === 'context') return
    const from = Math.max(0, index - CONTEXT_LINES)
    const to = Math.min(walked.length - 1, index + CONTEXT_LINES)
    const previous = ranges[ranges.length - 1]
    // Ranges that touch or overlap describe changes close enough to share context.
    if (previous !== undefined && from <= previous.to + 1) {
      previous.to = Math.max(previous.to, to)
      return
    }
    ranges.push({ from, to })
  })
  return ranges
}

/**
 * Compare two versions of a note as lines of raw text.
 *
 * Removed lines are the first argument, added lines the second, so reading a
 * diff top to bottom reads as replacing the former with the latter.
 */
export function diffLines(before: string, after: string): DiffHunk[] {
  if (before === after) return []

  // Empty content is no lines at all, where splitting would yield one blank one.
  const lines = (content: string) => (content === '' ? [] : content.split('\n'))
  const sides = trimUnchangedEdges(lines(before), lines(after))
  const walked = walk(sides.before, sides.after, sides.firstBeforeLine)
  const ranges = keptRanges(walked)

  return ranges.map(({ from, to }) => ({
    header: ranges.length > 1 ? walked[from]!.beforeLine : undefined,
    lines: walked.slice(from, to + 1).map(({ kind, text }) => ({ kind, text })),
  }))
}
