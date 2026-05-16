<template>
  <div
    v-if="!isMdOrLarger && opened"
    class="fixed inset-0 bg-black/50 z-30"
    aria-hidden="true"
    @click="closeOverlay"
  />
  <div
    class="flex flex-1 min-h-0 relative"
    :aria-busy="fullLayoutBusy ? true : undefined"
  >
    <div
      v-if="fullLayoutBusy"
      class="book-reading-layout-full-busy absolute inset-0 z-50 flex items-center justify-center bg-base-100/70"
      data-testid="book-reading-layout-full-busy"
      aria-hidden="true"
    >
      <span
        class="daisy-loading daisy-loading-spinner daisy-loading-lg"
      />
    </div>
    <aside
      :id="panelId"
      ref="asideRef"
      data-testid="book-reading-book-layout-aside"
      :class="[
        'relative bg-base-200 w-72 min-w-[16rem] max-w-[min(20rem,85vw)] transition-transform ease-in-out duration-200 overflow-y-auto overflow-x-hidden',
        isMdOrLarger
          ? opened
            ? 'shrink-0 border-r border-base-300'
            : 'hidden'
          : opened
            ? 'translate-x-0 fixed top-0 left-0 z-40 h-full pt-[env(safe-area-inset-top)]'
            : '-translate-x-full fixed top-0 left-0 z-40 h-full',
      ]"
    >
      <div
        v-if="fullLayoutBusy && !isMdOrLarger && opened"
        class="book-reading-layout-full-busy absolute inset-0 z-50 flex items-center justify-center bg-base-200/70"
        data-testid="book-reading-layout-aside-full-busy"
        aria-hidden="true"
      >
        <span
          class="daisy-loading daisy-loading-spinner daisy-loading-lg"
        />
      </div>
      <div
        data-testid="book-reading-book-layout"
        class="p-3 pb-8"
      >
        <button
          type="button"
          data-testid="book-reading-ai-reorganize-layout"
          class="daisy-btn daisy-btn-sm daisy-btn-outline daisy-btn-primary mb-3 w-full"
          :disabled="layoutActionsLocked"
          @click="emit('requestAiReorganize')"
        >
          AI Reorganize
        </button>
        <button
          v-for="block in blocks"
          :key="block.id"
          type="button"
          data-testid="book-reading-book-block"
          class="book-reading-book-block"
          :class="{
            'book-reading-book-block--pending':
              block.id === pendingLayoutBlockId,
          }"
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
          :aria-busy="block.id === pendingLayoutBlockId ? true : undefined"
          @click="onBlockRowClick(block, $event)"
          @pointerdown="onBlockPointerDown(block, $event)"
          @pointermove="onBlockPointerMove(block, $event)"
          @pointerup="onBlockPointerUp(block, $event)"
          @pointercancel="onBlockPointerCancel(block, $event)"
          @keydown.tab.shift.prevent="onBlockKeyOutdent(block)"
          @keydown.tab.exact.prevent="onBlockKeyIndent(block)"
          @keydown.delete.prevent="onBlockKeyCancel(block)"
        >
          <span
            v-if="block.id === pendingLayoutBlockId"
            class="book-reading-book-block-pending-overlay"
            data-testid="book-reading-book-block-layout-pending"
            aria-hidden="true"
          >
            <span
              class="daisy-loading daisy-loading-spinner daisy-loading-md"
            />
          </span>
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
              class="sr-only"
            >
              Marked as read
            </span>
            <span
              v-else-if="dispositionForBlock(block.id) === 'SKIMMED'"
              class="sr-only"
            >
              Marked as skimmed
            </span>
            <span
              v-else-if="dispositionForBlock(block.id) === 'SKIPPED'"
              class="sr-only"
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
import { blockStartEpubDisplayHref } from "@/lib/book-reading/asEpubLocator"
import {
  BOOK_LAYOUT_BLOCK_DRAG_THRESHOLD_PX,
  bookLayoutBlockDragIntent,
  bookLayoutBlockDragShouldCapture,
} from "@/lib/book-reading/bookLayoutBlockDragIntent"
import type { BookBlockReadingDisposition } from "@/lib/book-reading/readBlockIdsFromRecords"
import type { BookBlockFull } from "@generated/doughnut-backend-api"
import { computed, ref, watch } from "vue"

const opened = defineModel<boolean>("opened", { required: true })

const props = withDefaults(
  defineProps<{
    panelId: string
    isMdOrLarger: boolean
    blocks: BookBlockFull[]
    currentBlockId: number | null
    selectedBlockId: number | null
    pendingLayoutBlockId?: number | null
    dispositionForBlock: (
      blockId: number
    ) => BookBlockReadingDisposition | undefined
    fullLayoutBusy?: boolean
  }>(),
  {
    pendingLayoutBlockId: null,
    fullLayoutBusy: false,
  }
)

const emit = defineEmits<{
  blockClick: [block: BookBlockFull]
  blockIndent: [block: BookBlockFull]
  blockOutdent: [block: BookBlockFull]
  blockCancel: [block: BookBlockFull]
  requestAiReorganize: []
}>()

const asideRef = ref<HTMLElement | null>(null)

const layoutActionsLocked = computed(
  () => props.pendingLayoutBlockId !== null || props.fullLayoutBusy
)

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

function onBlockKeyIndent(block: BookBlockFull) {
  if (layoutActionsLocked.value) {
    return
  }
  emit("blockIndent", block)
}

function onBlockKeyOutdent(block: BookBlockFull) {
  if (layoutActionsLocked.value) {
    return
  }
  emit("blockOutdent", block)
}

function onBlockKeyCancel(block: BookBlockFull) {
  if (layoutActionsLocked.value) {
    return
  }
  emit("blockCancel", block)
}

function onBlockRowClick(block: BookBlockFull, e: MouseEvent) {
  if (layoutActionsLocked.value) {
    e.preventDefault()
    e.stopPropagation()
    return
  }
  if (suppressNextBlockClick.value) {
    suppressNextBlockClick.value = false
    e.preventDefault()
    e.stopPropagation()
    return
  }
  emit("blockClick", block)
}

function onBlockPointerDown(block: BookBlockFull, e: PointerEvent) {
  if (layoutActionsLocked.value) {
    return
  }
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
  if (layoutActionsLocked.value) {
    return
  }
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
  if (layoutActionsLocked.value) {
    blockPointerDrag.value = null
    return
  }
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
  if (layoutActionsLocked.value) {
    blockPointerDrag.value = null
    return
  }
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
@reference "@/assets/daisyui.css";

aside {
  max-height: 100%;
}

.book-reading-book-block--pending {
  @apply relative;
}

.book-reading-book-block-pending-overlay {
  @apply absolute inset-0 z-[1] flex items-center justify-center bg-base-200/70;
}

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
