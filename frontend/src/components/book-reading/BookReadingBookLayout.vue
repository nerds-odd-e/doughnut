<template>
  <div
    v-if="!isMdOrLarger && opened"
    class="daisy-fixed daisy-inset-0 daisy-bg-black/50 daisy-z-30"
    aria-hidden="true"
    @click="closeOverlay"
  />
  <div class="daisy-flex daisy-flex-1 daisy-min-h-0 daisy-relative">
    <aside
      :id="panelId"
      ref="asideRef"
      data-testid="book-reading-book-layout-aside"
      :class="[
        'daisy-bg-base-200 daisy-w-72 daisy-min-w-[16rem] daisy-max-w-[min(20rem,85vw)] daisy-transition-transform daisy-ease-in-out daisy-duration-200 daisy-overflow-y-auto daisy-overflow-x-hidden',
        isMdOrLarger
          ? opened
            ? 'daisy-relative daisy-shrink-0 daisy-border-r daisy-border-base-300'
            : 'daisy-hidden'
          : opened
            ? 'daisy-translate-x-0 daisy-fixed daisy-top-0 daisy-left-0 daisy-z-40 daisy-h-full daisy-pt-[env(safe-area-inset-top)]'
            : '-daisy-translate-x-full daisy-fixed daisy-top-0 daisy-left-0 daisy-z-40 daisy-h-full',
      ]"
    >
      <div
        data-testid="book-reading-book-layout"
        class="daisy-p-3 daisy-pb-8"
      >
        <button
          v-for="block in blocks"
          :key="block.id"
          type="button"
          data-testid="book-reading-book-block"
          class="book-reading-book-block"
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
          @click="onBlockRowClick(block, $event)"
          @pointerdown="onBlockPointerDown(block, $event)"
          @pointermove="onBlockPointerMove(block, $event)"
          @pointerup="onBlockPointerUp(block, $event)"
          @pointercancel="onBlockPointerCancel(block, $event)"
          @keydown.tab.shift.prevent="emit('blockOutdent', block)"
          @keydown.tab.exact.prevent="emit('blockIndent', block)"
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
            <span
              v-if="dispositionForBlock(block.id) === 'READ'"
              class="daisy-sr-only"
            >
              Marked as read
            </span>
            <span
              v-else-if="dispositionForBlock(block.id) === 'SKIMMED'"
              class="daisy-sr-only"
            >
              Marked as skimmed
            </span>
            <span
              v-else-if="dispositionForBlock(block.id) === 'SKIPPED'"
              class="daisy-sr-only"
            >
              Marked as skipped
            </span>
          </span>
        </button>
      </div>
    </aside>
    <slot />
  </div>
</template>

<script setup lang="ts">
import {
  BOOK_LAYOUT_BLOCK_DRAG_THRESHOLD_PX,
  bookLayoutBlockDragIntent,
  bookLayoutBlockDragShouldCapture,
} from "@/lib/book-reading/bookLayoutBlockDragIntent"
import type { BookBlockReadingDisposition } from "@/lib/book-reading/readBlockIdsFromRecords"
import type { BookBlockFull } from "@generated/doughnut-backend-api"
import { ref, watch } from "vue"

const opened = defineModel<boolean>("opened", { required: true })

const props = defineProps<{
  panelId: string
  isMdOrLarger: boolean
  blocks: BookBlockFull[]
  currentBlockId: number | null
  selectedBlockId: number | null
  dispositionForBlock: (
    blockId: number
  ) => BookBlockReadingDisposition | undefined
}>()

const emit = defineEmits<{
  blockClick: [block: BookBlockFull]
  blockIndent: [block: BookBlockFull]
  blockOutdent: [block: BookBlockFull]
}>()

const asideRef = ref<HTMLElement | null>(null)

const dragThresholdOpts = { thresholdPx: BOOK_LAYOUT_BLOCK_DRAG_THRESHOLD_PX }

const blockPointerDrag = ref<{
  blockId: number
  startX: number
  startY: number
  pointerId: number
  captured: boolean
} | null>(null)

const suppressNextBlockClick = ref(false)

function closeOverlay() {
  opened.value = false
}

function onBlockRowClick(block: BookBlockFull, e: MouseEvent) {
  if (suppressNextBlockClick.value) {
    suppressNextBlockClick.value = false
    e.preventDefault()
    e.stopPropagation()
    return
  }
  emit("blockClick", block)
}

function onBlockPointerDown(block: BookBlockFull, e: PointerEvent) {
  if (e.pointerType === "mouse" && e.button !== 0) {
    return
  }
  blockPointerDrag.value = {
    blockId: block.id,
    startX: e.clientX,
    startY: e.clientY,
    pointerId: e.pointerId,
    captured: false,
  }
}

function onBlockPointerMove(block: BookBlockFull, e: PointerEvent) {
  const s = blockPointerDrag.value
  if (!s || s.pointerId !== e.pointerId || s.blockId !== block.id) {
    return
  }
  const dx = e.clientX - s.startX
  const dy = e.clientY - s.startY
  if (
    !s.captured &&
    bookLayoutBlockDragShouldCapture(dx, dy, dragThresholdOpts)
  ) {
    try {
      ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
    } catch {
      /* Synthetic pointer events in tests may not have an active pointer id. */
    }
    blockPointerDrag.value = { ...s, captured: true }
  }
}

function onBlockPointerUp(block: BookBlockFull, e: PointerEvent) {
  const s = blockPointerDrag.value
  if (!s || s.pointerId !== e.pointerId || s.blockId !== block.id) {
    return
  }
  const target = e.currentTarget as HTMLElement
  const dx = e.clientX - s.startX
  const dy = e.clientY - s.startY
  if (s.captured) {
    try {
      target.releasePointerCapture(e.pointerId)
    } catch {
      /* ignore */
    }
  }
  blockPointerDrag.value = null
  const intent = bookLayoutBlockDragIntent(dx, dy, dragThresholdOpts)
  if (intent === "INDENT") {
    suppressNextBlockClick.value = true
    emit("blockIndent", block)
  } else if (intent === "OUTDENT") {
    suppressNextBlockClick.value = true
    emit("blockOutdent", block)
  }
}

function onBlockPointerCancel(block: BookBlockFull, e: PointerEvent) {
  const s = blockPointerDrag.value
  if (!s || s.pointerId !== e.pointerId || s.blockId !== block.id) {
    return
  }
  if (s.captured) {
    try {
      ;(e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId)
    } catch {
      /* ignore */
    }
  }
  blockPointerDrag.value = null
}

watch(
  () => props.currentBlockId,
  (id) => {
    if (id === null || !opened.value) {
      return
    }
    requestAnimationFrame(() => {
      if (!opened.value) {
        return
      }
      const row = asideRef.value?.querySelector('[data-current-block="true"]')
      row?.scrollIntoView({ block: "nearest", inline: "nearest" })
    })
  },
  { flush: "post" }
)

watch(
  () => props.selectedBlockId,
  (id) => {
    if (id === null || !opened.value) {
      return
    }
    requestAnimationFrame(() => {
      if (!opened.value) {
        return
      }
      const row = asideRef.value?.querySelector(
        '[data-current-selection="true"]'
      )
      if (row instanceof HTMLElement) {
        row.focus()
      }
    })
  },
  { flush: "post" }
)
</script>

<style scoped>
aside {
  max-height: 100%;
}

.book-reading-book-block {
  @apply daisy-flex daisy-w-full daisy-min-h-10 daisy-items-stretch daisy-gap-1 daisy-text-left daisy-rounded-none;
  @apply daisy-border-0 daisy-border-solid daisy-border-l-4 daisy-border-transparent;
  @apply daisy-py-0 daisy-pr-2 daisy-pl-1 daisy-text-sm daisy-leading-snug daisy-font-normal;
  @apply daisy-transition-colors daisy-duration-150;
  @apply hover:daisy-bg-base-300/55;
  @apply focus:daisy-outline-none focus-visible:daisy-ring-2 focus-visible:daisy-ring-primary/50;
  @apply focus-visible:daisy-ring-offset-2 focus-visible:daisy-ring-offset-base-200;
}

.book-reading-book-block-guides {
  @apply daisy-flex daisy-shrink-0 daisy-items-stretch;
}

.book-reading-book-block-guide {
  @apply daisy-flex daisy-w-3 daisy-shrink-0 daisy-flex-col daisy-items-center daisy-self-stretch daisy-min-h-0;
}

.book-reading-book-block-guide-line {
  @apply daisy-w-0.5 daisy-min-h-0 daisy-flex-1 daisy-rounded-none daisy-bg-base-content/25;
}

.book-reading-book-block-title {
  @apply daisy-min-w-0 daisy-flex-1 daisy-py-2 daisy-pl-0 daisy-text-left;
}

.book-reading-book-block[data-current-block="true"] {
  @apply daisy-bg-primary/35;
}

.book-reading-book-block[data-current-selection="true"] {
  @apply daisy-border-primary daisy-font-medium;
}

.book-reading-book-block[data-direct-content-read="true"] {
  @apply daisy-border-r-4 daisy-border-r-success;
}

.book-reading-book-block[data-direct-content-skimmed="true"] {
  @apply daisy-border-r-4 daisy-border-r-warning;
}

.book-reading-book-block[data-direct-content-skipped="true"] {
  @apply daisy-border-r-4 daisy-border-r-neutral;
}
</style>
