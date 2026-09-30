<template>
  <div class="flex flex-1 min-h-0 relative">
    <SidebarDrawer
      :id="panelId"
      v-model:opened="opened"
      :is-md-or-larger="isMdOrLarger"
      data-testid="book-reading-book-layout-aside"
      class="bg-base-200 w-72 min-w-[16rem] max-w-[min(20rem,85vw)] shrink-0 border-r border-base-300 transition-transform ease-in-out duration-200 overflow-y-auto overflow-x-hidden"
    >
      <div
        ref="layoutRef"
        data-testid="book-reading-book-layout"
        class="p-3 pb-8"
      >
        <button
          type="button"
          data-testid="book-reading-ai-reorganize-layout"
          class="daisy-btn daisy-btn-sm daisy-btn-outline daisy-btn-primary mb-3 w-full"
          @click="emit('requestAiReorganize')"
        >
          AI Reorganize
        </button>
        <button
          v-if="canUndoDepthChange"
          type="button"
          data-testid="book-reading-undo-layout-change"
          class="daisy-btn daisy-btn-sm daisy-btn-outline mb-3 w-full"
          @click="emit('undoDepthChange')"
        >
          Undo
        </button>
        <template v-for="block in blocks" :key="block.id">
          <button
            type="button"
            data-testid="book-reading-book-block"
            class="book-reading-book-block"
            :data-epub-start-href="blockStartEpubDisplayHref(block) ?? undefined"
            :data-book-block-depth="block.depth"
            :data-current-block="
              block.id === currentBlockId ? 'true' : undefined
            "
            :data-current-selection="
              block.id === selectedBlockId ? 'true' : undefined
            "
            :data-direct-content-read="
              dispositionForBlock(block.id) === 'READ' ? 'true' : undefined
            "
            :data-direct-content-skimmed="
              dispositionForBlock(block.id) === 'SKIMMED' ? 'true' : undefined
            "
            :data-direct-content-skipped="
              dispositionForBlock(block.id) === 'SKIPPED' ? 'true' : undefined
            "
            :aria-current="
              block.id === currentBlockId ? 'location' : undefined
            "
            :tabindex="block.id === tabStopBlockId ? 0 : -1"
            @focusin="lastFocusedBlockId = block.id"
            @click="onBlockRowClick(block, $event)"
            @pointerdown="blockDrag.onPointerDown(block, $event)"
            @pointermove="blockDrag.onPointerMove(block, $event)"
            @pointerup="blockDrag.onPointerUp(block, $event)"
            @pointercancel="blockDrag.onPointerCancel(block, $event)"
            @keydown.down.exact.prevent="focusRowBy($event, 1)"
            @keydown.up.exact.prevent="focusRowBy($event, -1)"
            @keydown.alt.shift.right.prevent="emit('blockIndent', block)"
            @keydown.alt.shift.left.prevent="emit('blockOutdent', block)"
            @keydown.delete.prevent="emit('blockCancel', block)"
          >
            <span
              class="book-reading-book-block-guides"
              data-testid="book-reading-book-block-guides"
              :data-book-block-guide-depth="block.depth"
              aria-hidden="true"
            >
              <span
                v-for="n in block.depth"
                :key="n"
                class="book-reading-book-block-guide"
                data-testid="book-reading-book-block-guide"
              >
                <span class="book-reading-book-block-guide-line" />
              </span>
            </span>
            <span class="book-reading-book-block-title">
              {{ block.title }}
              <span v-if="dispositionForBlock(block.id)" class="sr-only">
                Marked as {{ dispositionForBlock(block.id)?.toLowerCase() }}
              </span>
            </span>
          </button>
          <BookBlockMarkControl
            v-if="block.id === selectedBlockId && dispositionForBlock(block.id)"
            :disposition="dispositionForBlock(block.id)!"
            @change="(status) => emit('changeMark', block.id, status)"
            @clear="emit('clearMark', block.id)"
          />
        </template>
      </div>
    </SidebarDrawer>
    <slot />
  </div>
</template>

<script setup lang="ts">
import BookBlockMarkControl from "@/components/book-reading/BookBlockMarkControl.vue"
import SidebarDrawer from "@/components/commons/SidebarDrawer.vue"
import { blockStartEpubDisplayHref } from "@/lib/book-reading/asEpubLocator"
import { useBookLayoutBlockPointerDrag } from "@/composables/book-reading/useBookLayoutBlockPointerDrag"
import type { BookBlockReadingDisposition } from "@/lib/book-reading/readBlockIdsFromRecords"
import type { BookBlockFull } from "@generated/donut-backend-api"
import { computed, ref, watch } from "vue"

const opened = defineModel<boolean>("opened", { required: true })

const props = withDefaults(
  defineProps<{
    panelId: string
    isMdOrLarger: boolean
    blocks: BookBlockFull[]
    currentBlockId: number | null
    selectedBlockId: number | null
    canUndoDepthChange?: boolean
    dispositionForBlock: (
      blockId: number
    ) => BookBlockReadingDisposition | undefined
  }>(),
  {}
)

const emit = defineEmits<{
  blockClick: [block: BookBlockFull]
  blockIndent: [block: BookBlockFull]
  blockOutdent: [block: BookBlockFull]
  blockCancel: [block: BookBlockFull]
  changeMark: [blockId: number, status: BookBlockReadingDisposition]
  clearMark: [blockId: number]
  requestAiReorganize: []
  undoDepthChange: []
}>()

const layoutRef = ref<HTMLElement | null>(null)

const lastFocusedBlockId = ref<number | null>(null)

const tabStopBlockId = computed(() => {
  const candidates = [
    lastFocusedBlockId.value,
    props.selectedBlockId,
    props.currentBlockId,
  ]
  return (
    candidates.find((id) => props.blocks.some((b) => b.id === id)) ??
    props.blocks[0]?.id
  )
})

function focusRowBy(e: KeyboardEvent, step: number) {
  const rows = [
    ...layoutRef.value!.querySelectorAll<HTMLElement>(
      '[data-testid="book-reading-book-block"]'
    ),
  ]
  rows[rows.indexOf(e.currentTarget as HTMLElement) + step]?.focus()
}

const blockDrag = useBookLayoutBlockPointerDrag({
  indent: (block) => emit("blockIndent", block),
  outdent: (block) => emit("blockOutdent", block),
})

function onBlockRowClick(block: BookBlockFull, e: MouseEvent) {
  if (blockDrag.consumeDragClick(e)) {
    return
  }
  emit("blockClick", block)
  if (!props.isMdOrLarger) {
    opened.value = false
  }
}

/** Once the open layout has painted, act on the row marked by `selector`. */
function onOpenLayoutRow(
  source: () => number | null,
  selector: string,
  act: (row: HTMLElement) => void
) {
  watch(
    source,
    (id) => {
      if (id === null || !opened.value) {
        return
      }
      requestAnimationFrame(() => {
        if (!opened.value) {
          return
        }
        const row = layoutRef.value?.querySelector(selector)
        if (row instanceof HTMLElement) {
          act(row)
        }
      })
    },
    { flush: "post" }
  )
}

onOpenLayoutRow(
  () => props.currentBlockId,
  '[data-current-block="true"]',
  (row) => row.scrollIntoView({ block: "nearest", inline: "nearest" })
)
onOpenLayoutRow(
  () => props.selectedBlockId,
  '[data-current-selection="true"]',
  (row) => row.focus()
)
</script>

<style scoped>
@reference "@/assets/daisyui.css";

.book-reading-book-block {
  @apply flex w-full min-h-10 items-stretch gap-1 text-left rounded-none;
  @apply border-0 border-solid border-l-4 border-transparent;
  @apply py-0 pr-2 pl-1 text-sm leading-snug font-normal;
  @apply transition-colors duration-150;
  @apply hover:bg-base-300/55;
  @apply focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/50;
  @apply focus-visible:ring-offset-2 focus-visible:ring-offset-base-200;
}

.book-reading-book-block-guides {
  @apply flex shrink-0 items-stretch;
}

.book-reading-book-block-guide {
  @apply flex w-3 shrink-0 flex-col items-center self-stretch min-h-0;
}

.book-reading-book-block-guide-line {
  @apply w-0.5 min-h-0 flex-1 rounded-none bg-base-content/25;
}

.book-reading-book-block-title {
  @apply min-w-0 flex-1 py-2 pl-0 text-left;
}

.book-reading-book-block[data-current-block="true"] {
  @apply bg-primary/35;
}

.book-reading-book-block[data-current-selection="true"] {
  @apply border-primary font-medium;
}

.book-reading-book-block[data-direct-content-read="true"] {
  @apply border-r-4 border-r-success;
}

.book-reading-book-block[data-direct-content-skimmed="true"] {
  @apply border-r-4 border-r-warning;
}

.book-reading-book-block[data-direct-content-skipped="true"] {
  @apply border-r-4 border-r-neutral;
}
</style>
