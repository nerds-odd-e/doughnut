<template>
  <div class="diff-view daisy-flex daisy-flex-col md:daisy-flex-row daisy-gap-2">
    <div class="daisy-flex-1 daisy-flex daisy-flex-col">
      <div class="daisy-text-sm daisy-font-semibold daisy-mb-2 daisy-text-error">
        Current
      </div>
      <div
        ref="leftPane"
        class="diff-pane daisy-border daisy-border-base-300 daisy-rounded daisy-bg-base-200 daisy-text-sm daisy-overflow-auto"
        :style="{ maxHeight: maxHeight }"
        @scroll="onLeftScroll"
        data-testid="diff-left-pane"
      >
        <table class="diff-table daisy-font-mono">
          <tbody>
            <tr
              v-for="(row, index) in pairedRows"
              :key="`left-${index}`"
              class="diff-row"
              :data-row-index="index"
            >
              <td
                class="diff-line-number"
                :class="{ 'diff-placeholder': row.left.isPlaceholder }"
                data-testid="line-number-left"
              >
                {{ row.left.isPlaceholder ? "" : row.left.lineNumber }}
              </td>
              <td
                class="diff-content-cell"
                :class="{
                  'diff-added': row.left.type === 'added',
                  'diff-placeholder': row.left.isPlaceholder,
                }"
                :data-placeholder="row.left.isPlaceholder || undefined"
              >
                <span v-if="!row.left.isPlaceholder">{{ row.left.text }}</span>
                <span v-else>&nbsp;</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div class="daisy-flex-1 daisy-flex daisy-flex-col">
      <div class="daisy-text-sm daisy-font-semibold daisy-mb-2 daisy-text-success">
        Will restore to
      </div>
      <div
        ref="rightPane"
        class="diff-pane daisy-border daisy-border-base-300 daisy-rounded daisy-bg-base-200 daisy-text-sm daisy-overflow-auto"
        :style="{ maxHeight: maxHeight }"
        @scroll="onRightScroll"
        data-testid="diff-right-pane"
      >
        <table class="diff-table daisy-font-mono">
          <tbody>
            <tr
              v-for="(row, index) in pairedRows"
              :key="`right-${index}`"
              class="diff-row"
              :data-row-index="index"
            >
              <td
                class="diff-line-number"
                :class="{ 'diff-placeholder': row.right.isPlaceholder }"
                data-testid="line-number-right"
              >
                {{ row.right.isPlaceholder ? "" : row.right.lineNumber }}
              </td>
              <td
                class="diff-content-cell"
                :class="{
                  'diff-removed': row.right.type === 'removed',
                  'diff-placeholder': row.right.isPlaceholder,
                }"
                :data-placeholder="row.right.isPlaceholder || undefined"
              >
                <span v-if="!row.right.isPlaceholder">{{ row.right.text }}</span>
                <span v-else>&nbsp;</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from "vue"

interface DiffLine {
  text: string
  lineNumber: number | null
  type: "same" | "added" | "removed"
  isPlaceholder: boolean
}

interface PairedRow {
  left: DiffLine
  right: DiffLine
}

const props = defineProps({
  current: { type: String, required: true },
  old: { type: String, required: true },
  maxHeight: { type: String, default: "200px" },
})

const leftPane = ref<HTMLElement | null>(null)
const rightPane = ref<HTMLElement | null>(null)
let isScrolling = false
let scrollTimeout: ReturnType<typeof setTimeout> | null = null

const onLeftScroll = () => {
  if (isScrolling) return
  syncScroll(leftPane.value, rightPane.value)
}

const onRightScroll = () => {
  if (isScrolling) return
  syncScroll(rightPane.value, leftPane.value)
}

const syncScroll = (source: HTMLElement | null, target: HTMLElement | null) => {
  if (!source || !target) return

  isScrolling = true
  target.scrollTop = source.scrollTop
  target.scrollLeft = source.scrollLeft

  if (scrollTimeout) clearTimeout(scrollTimeout)
  scrollTimeout = setTimeout(() => {
    isScrolling = false
  }, 50)
}

onMounted(() => {
  if (leftPane.value) {
    leftPane.value.addEventListener("scroll", onLeftScroll, { passive: true })
  }
  if (rightPane.value) {
    rightPane.value.addEventListener("scroll", onRightScroll, { passive: true })
  }
})

onUnmounted(() => {
  if (scrollTimeout) clearTimeout(scrollTimeout)
})

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

const pairedRows = computed<PairedRow[]>(() => {
  if (props.current === props.old) {
    const lines = props.current.split("\n")
    return lines.map((line, idx) => ({
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

  const { currentLines, oldLines } = computeLineDiff(props.current, props.old)
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
      result.push({
        left: leftLine,
        right: {
          text: "",
          lineNumber: null,
          type: "same",
          isPlaceholder: true,
        },
      })
      li++
    } else if (rightLine && rightLine.type === "removed") {
      result.push({
        left: {
          text: "",
          lineNumber: null,
          type: "same",
          isPlaceholder: true,
        },
        right: rightLine,
      })
      ri++
    } else {
      if (leftLine) {
        result.push({
          left: leftLine,
          right: {
            text: "",
            lineNumber: null,
            type: "same",
            isPlaceholder: true,
          },
        })
        li++
      }
      if (rightLine) {
        result.push({
          left: {
            text: "",
            lineNumber: null,
            type: "same",
            isPlaceholder: true,
          },
          right: rightLine,
        })
        ri++
      }
    }
  }

  return result
})
</script>

<style scoped>
.diff-view {
  min-height: 100px;
}

.diff-pane {
  min-height: 80px;
  overflow: auto;
}

.diff-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.diff-row {
  line-height: 1.4;
}

.diff-line-number {
  width: 3em;
  min-width: 3em;
  max-width: 3em;
  padding: 0 0.5em;
  text-align: right;
  color: #6b7280;
  background-color: rgba(0, 0, 0, 0.05);
  border-right: 1px solid #d1d5db;
  user-select: none;
  vertical-align: top;
}

.diff-line-number.diff-placeholder {
  background-color: rgba(0, 0, 0, 0.02);
}

.diff-content-cell {
  padding: 0 0.5em;
  white-space: pre;
  overflow-x: auto;
  vertical-align: top;
}

.diff-content-cell.diff-added {
  background-color: #fee2e2;
  color: #991b1b;
  text-decoration: line-through;
}

.diff-content-cell.diff-removed {
  background-color: #dcfce7;
  color: #166534;
}

.diff-content-cell.diff-placeholder {
  background-color: #f3f4f6;
  min-height: 1.4em;
  height: 1.4em;
}

.diff-row {
  height: 1.4em;
}

.diff-placeholder td {
  height: 1.4em;
}
</style>
