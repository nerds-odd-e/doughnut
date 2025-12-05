<template>
  <div class="diff-view daisy-flex daisy-flex-col md:daisy-flex-row daisy-gap-2">
    <div class="daisy-flex-1">
      <div class="daisy-text-sm daisy-font-semibold daisy-mb-2 daisy-text-error">
        Current
      </div>
      <div
        class="diff-content daisy-border daisy-border-base-300 daisy-rounded daisy-p-2 daisy-bg-base-200 daisy-text-sm daisy-overflow-auto"
        :style="{ maxHeight: maxHeight }"
      >
        <div class="daisy-whitespace-pre-wrap daisy-font-mono" v-html="highlightedCurrent"></div>
      </div>
    </div>
    <div class="daisy-flex-1">
      <div class="daisy-text-sm daisy-font-semibold daisy-mb-2 daisy-text-success">
        Will restore to
      </div>
      <div
        class="diff-content daisy-border daisy-border-base-300 daisy-rounded daisy-p-2 daisy-bg-base-200 daisy-text-sm daisy-overflow-auto"
        :style="{ maxHeight: maxHeight }"
      >
        <div class="daisy-whitespace-pre-wrap daisy-font-mono" v-html="highlightedOld"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"

const props = defineProps({
  current: { type: String, required: true },
  old: { type: String, required: true },
  maxHeight: { type: String, default: "200px" },
})

const escapeHtml = (text: string): string => {
  const div = document.createElement("div")
  div.textContent = text
  return div.innerHTML
}

const computeDiff = (current: string, old: string) => {
  // Simple word-based diff
  const currentWords = current.split(/(\s+)/)
  const oldWords = old.split(/(\s+)/)

  const result: Array<{ text: string; type: "same" | "removed" | "added" }> = []
  let i = 0
  let j = 0

  while (i < currentWords.length || j < oldWords.length) {
    if (i >= currentWords.length) {
      // Only in old
      result.push({ text: oldWords[j] || "", type: "removed" })
      j++
    } else if (j >= oldWords.length) {
      // Only in current
      result.push({ text: currentWords[i] || "", type: "added" })
      i++
    } else if (currentWords[i] === oldWords[j]) {
      // Same
      result.push({ text: currentWords[i] || "", type: "same" })
      i++
      j++
    } else {
      // Different - try to find next match
      let found = false
      for (let k = j + 1; k < oldWords.length && k < j + 10; k++) {
        if (currentWords[i] === oldWords[k]) {
          // Found match ahead in old
          for (let l = j; l < k; l++) {
            result.push({ text: oldWords[l] || "", type: "removed" })
          }
          j = k
          found = true
          break
        }
      }
      if (!found) {
        for (let k = i + 1; k < currentWords.length && k < i + 10; k++) {
          if (currentWords[k] === oldWords[j]) {
            // Found match ahead in current
            for (let l = i; l < k; l++) {
              result.push({ text: currentWords[l] || "", type: "added" })
            }
            i = k
            found = true
            break
          }
        }
      }
      if (!found) {
        // No match found, mark both as different
        result.push({ text: currentWords[i] || "", type: "added" })
        result.push({ text: oldWords[j] || "", type: "removed" })
        i++
        j++
      }
    }
  }

  return result
}

const highlightedCurrent = computed(() => {
  if (props.current === props.old) {
    return escapeHtml(props.current)
  }

  const diff = computeDiff(props.current, props.old)
  return diff
    .filter((item) => item.type !== "removed")
    .map((item) => {
      const escaped = escapeHtml(item.text)
      if (item.type === "added") {
        return `<span class="diff-added">${escaped}</span>`
      }
      return escaped
    })
    .join("")
})

const highlightedOld = computed(() => {
  if (props.current === props.old) {
    return escapeHtml(props.old)
  }

  const diff = computeDiff(props.current, props.old)
  return diff
    .filter((item) => item.type !== "added")
    .map((item) => {
      const escaped = escapeHtml(item.text)
      if (item.type === "removed") {
        return `<span class="diff-removed">${escaped}</span>`
      }
      return escaped
    })
    .join("")
})
</script>

<style scoped>
.diff-view {
  min-height: 100px;
}

.diff-content {
  min-height: 80px;
}

:deep(.diff-added) {
  background-color: #fee2e2;
  color: #991b1b;
  text-decoration: line-through;
}

:deep(.diff-removed) {
  background-color: #dcfce7;
  color: #166534;
}
</style>

