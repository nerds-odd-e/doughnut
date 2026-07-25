export interface DiffLine {
  text: string
  lineNumber: number | null
  type: "same" | "added" | "removed"
  isPlaceholder: boolean
}

export interface PairedRow {
  left: DiffLine
  right: DiffLine
}

interface LCSMatch {
  currentIdx: number
  oldIdx: number
}

const computeLCS = (current: string[], old: string[]): LCSMatch[] => {
  const m = current.length
  const n = old.length

  const dp: number[][] = Array(m + 1)
    .fill(null)
    .map(() => Array(n + 1).fill(0))

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (current[i - 1] === old[j - 1]) {
        dp[i]![j] = (dp[i - 1]?.[j - 1] ?? 0) + 1
      } else {
        dp[i]![j] = Math.max(dp[i - 1]?.[j] ?? 0, dp[i]?.[j - 1] ?? 0)
      }
    }
  }

  const result: LCSMatch[] = []
  let i = m
  let j = n
  while (i > 0 && j > 0) {
    if (current[i - 1] === old[j - 1]) {
      result.unshift({ currentIdx: i - 1, oldIdx: j - 1 })
      i--
      j--
    } else if ((dp[i - 1]?.[j] ?? 0) > (dp[i]?.[j - 1] ?? 0)) {
      i--
    } else {
      j--
    }
  }

  return result
}

const computeLineDiff = (
  currentText: string,
  oldText: string
): { currentLines: DiffLine[]; oldLines: DiffLine[] } => {
  const currentLines = currentText.split("\n")
  const oldLines = oldText.split("\n")

  const lcs = computeLCS(currentLines, oldLines)

  const currentResult: DiffLine[] = []
  const oldResult: DiffLine[] = []

  let ci = 0
  let oi = 0
  let lcsIdx = 0

  while (ci < currentLines.length || oi < oldLines.length) {
    const match = lcs[lcsIdx]
    if (match) {
      while (ci < match.currentIdx) {
        currentResult.push({
          text: currentLines[ci] ?? "",
          lineNumber: ci + 1,
          type: "added",
          isPlaceholder: false,
        })
        ci++
      }

      while (oi < match.oldIdx) {
        oldResult.push({
          text: oldLines[oi] ?? "",
          lineNumber: oi + 1,
          type: "removed",
          isPlaceholder: false,
        })
        oi++
      }

      currentResult.push({
        text: currentLines[ci] ?? "",
        lineNumber: ci + 1,
        type: "same",
        isPlaceholder: false,
      })
      oldResult.push({
        text: oldLines[oi] ?? "",
        lineNumber: oi + 1,
        type: "same",
        isPlaceholder: false,
      })
      ci++
      oi++
      lcsIdx++
    } else {
      while (ci < currentLines.length) {
        currentResult.push({
          text: currentLines[ci] ?? "",
          lineNumber: ci + 1,
          type: "added",
          isPlaceholder: false,
        })
        ci++
      }
      while (oi < oldLines.length) {
        oldResult.push({
          text: oldLines[oi] ?? "",
          lineNumber: oi + 1,
          type: "removed",
          isPlaceholder: false,
        })
        oi++
      }
    }
  }

  return { currentLines: currentResult, oldLines: oldResult }
}

const placeholderLine = (): DiffLine => ({
  text: "",
  lineNumber: null,
  type: "same",
  isPlaceholder: true,
})

export const buildPairedDiffRows = (
  current: string,
  old: string
): PairedRow[] => {
  if (current === old) {
    return current.split("\n").map((line, idx) => ({
      left: {
        text: line,
        lineNumber: idx + 1,
        type: "same" as const,
        isPlaceholder: false,
      },
      right: {
        text: line,
        lineNumber: idx + 1,
        type: "same" as const,
        isPlaceholder: false,
      },
    }))
  }

  const { currentLines, oldLines } = computeLineDiff(current, old)
  const result: PairedRow[] = []

  let li = 0
  let ri = 0

  while (li < currentLines.length || ri < oldLines.length) {
    const leftLine = currentLines[li]
    const rightLine = oldLines[ri]

    if (
      leftLine &&
      rightLine &&
      leftLine.type === "same" &&
      rightLine.type === "same"
    ) {
      result.push({ left: leftLine, right: rightLine })
      li++
      ri++
    } else if (leftLine && leftLine.type === "added") {
      result.push({ left: leftLine, right: placeholderLine() })
      li++
    } else if (rightLine && rightLine.type === "removed") {
      result.push({ left: placeholderLine(), right: rightLine })
      ri++
    } else {
      if (leftLine) {
        result.push({ left: leftLine, right: placeholderLine() })
        li++
      }
      if (rightLine) {
        result.push({ left: placeholderLine(), right: rightLine })
        ri++
      }
    }
  }

  return result
}
