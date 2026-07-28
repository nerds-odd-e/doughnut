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
  after: readonly string[]
): WalkedLine[] {
  const table = lcsLengths(before, after)
  const walked: WalkedLine[] = []
  let i = 0
  let j = 0
  while (i < before.length && j < after.length) {
    if (before[i] === after[j]) {
      walked.push({ kind: 'context', text: before[i]!, beforeLine: i + 1 })
      i++
      j++
    } else if (table[i + 1]![j]! >= table[i]![j + 1]!) {
      walked.push({ kind: 'removed', text: before[i]!, beforeLine: i + 1 })
      i++
    } else {
      walked.push({ kind: 'added', text: after[j]!, beforeLine: i + 1 })
      j++
    }
  }
  while (i < before.length) {
    walked.push({ kind: 'removed', text: before[i]!, beforeLine: i + 1 })
    i++
  }
  while (j < after.length) {
    walked.push({ kind: 'added', text: after[j]!, beforeLine: i + 1 })
    j++
  }
  return walked
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
  const walked = walk(lines(before), lines(after))
  const ranges = keptRanges(walked)

  return ranges.map(({ from, to }) => ({
    header: ranges.length > 1 ? walked[from]!.beforeLine : undefined,
    lines: walked.slice(from, to + 1).map(({ kind, text }) => ({ kind, text })),
  }))
}
