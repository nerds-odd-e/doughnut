<template>
  <div class="diff-view flex flex-col md:flex-row gap-2">
    <DiffPane
      ref="leftPane"
      :label="currentLabel"
      label-class="text-error"
      :max-height="maxHeight"
      :rows="leftRows"
      side="left"
      highlight-type="added"
      highlight-class="diff-added"
      @scroll="onLeftScroll"
    />
    <DiffPane
      ref="rightPane"
      :label="oldLabel"
      label-class="text-success"
      :max-height="maxHeight"
      :rows="rightRows"
      side="right"
      highlight-type="removed"
      highlight-class="diff-removed"
      @scroll="onRightScroll"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue"
import DiffPane from "./DiffPane.vue"
import { buildPairedDiffRows } from "./diffViewPairedRows"

const props = defineProps({
  current: { type: String, required: true },
  old: { type: String, required: true },
  maxHeight: { type: String, default: "200px" },
  currentLabel: { type: String, default: "Current" },
  oldLabel: { type: String, default: "Will restore to" },
})

const leftPane = ref<{ pane: HTMLElement | null } | null>(null)
const rightPane = ref<{ pane: HTMLElement | null } | null>(null)
let isScrolling = false
let scrollTimeout: ReturnType<typeof setTimeout> | null = null

const onLeftScroll = () => {
  if (isScrolling) return
  syncScroll(leftPane.value?.pane ?? null, rightPane.value?.pane ?? null)
}

const onRightScroll = () => {
  if (isScrolling) return
  syncScroll(rightPane.value?.pane ?? null, leftPane.value?.pane ?? null)
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

const pairedRows = computed(() => buildPairedDiffRows(props.current, props.old))
const leftRows = computed(() => pairedRows.value.map((row) => row.left))
const rightRows = computed(() => pairedRows.value.map((row) => row.right))
</script>

<style scoped>
.diff-view {
  min-height: 100px;
}
</style>
