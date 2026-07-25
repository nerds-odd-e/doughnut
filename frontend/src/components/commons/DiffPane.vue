<template>
  <div class="flex-1 flex flex-col">
    <div class="text-sm font-semibold mb-2" :class="labelClass">
      {{ label }}
    </div>
    <div
      ref="pane"
      class="diff-pane border border-base-300 rounded bg-base-200 text-sm overflow-auto"
      :style="{ maxHeight: maxHeight }"
      @scroll="$emit('scroll')"
      :data-testid="paneTestId"
    >
      <table class="diff-table font-mono">
        <tbody>
          <tr
            v-for="(row, index) in rows"
            :key="`${side}-${index}`"
            class="diff-row"
            :data-row-index="index"
          >
            <td
              class="diff-line-number"
              :class="{ 'diff-placeholder': row.isPlaceholder }"
              :data-testid="lineNumberTestId"
            >
              {{ row.isPlaceholder ? "" : row.lineNumber }}
            </td>
            <td
              class="diff-content-cell"
              :class="{
                [highlightClass]: row.type === highlightType,
                'diff-placeholder': row.isPlaceholder,
              }"
              :data-placeholder="row.isPlaceholder || undefined"
            >
              <span v-if="!row.isPlaceholder">{{ row.text }}</span>
              <span v-else>&nbsp;</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue"
import type { DiffLine } from "./diffViewPairedRows"

const props = defineProps<{
  label: string
  labelClass: string
  maxHeight: string
  rows: DiffLine[]
  side: "left" | "right"
  highlightType: "added" | "removed"
  highlightClass: string
}>()

defineEmits<{
  scroll: []
}>()

const pane = ref<HTMLElement | null>(null)

const paneTestId = computed(() =>
  props.side === "left" ? "diff-left-pane" : "diff-right-pane"
)
const lineNumberTestId = computed(() =>
  props.side === "left" ? "line-number-left" : "line-number-right"
)

defineExpose({
  pane,
})
</script>

<style scoped>
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
  height: 1.4em;
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

.diff-placeholder td {
  height: 1.4em;
}
</style>
