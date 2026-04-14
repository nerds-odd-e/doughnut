<template>
  <section
    data-testid="book-reading-content-stream"
    class="daisy-shrink-0 daisy-max-h-[30vh] daisy-min-h-0 daisy-overflow-y-auto daisy-border-t daisy-border-base-300 daisy-bg-base-200/80 daisy-px-2 daisy-py-2"
    :aria-label="ariaLabel"
    @keydown.escape="onEscape"
  >
    <p
      v-if="selectedBlockTitle"
      class="daisy-text-xs daisy-font-medium daisy-text-base-content/70 daisy-mb-1 daisy-truncate"
    >
      {{ selectedBlockTitle }}
    </p>
    <ul
      v-if="contentBlocks.length > 0"
      class="daisy-m-0 daisy-list-none daisy-space-y-1 daisy-p-0"
    >
      <template v-for="block in contentBlocks" :key="block.id">
        <li
          data-testid="book-reading-content-block"
          :data-book-content-block-id="String(block.id)"
          class="daisy-text-sm daisy-text-base-content daisy-rounded daisy-bg-base-100 daisy-px-2 daisy-py-1 daisy-select-none"
          :class="{ 'daisy-opacity-60': disabled }"
          @pointerdown="onContentBlockPointerDown(block, $event)"
          @pointermove="onContentBlockPointerMove(block, $event)"
          @pointerup="onContentBlockPointerUp(block, $event)"
          @pointercancel="onContentBlockPointerCancel(block, $event)"
        >
          {{ previewTextFromContentBlockRaw(block) }}
        </li>
        <li
          v-if="calloutBlockId === block.id"
          key="callout"
          class="daisy-py-1"
          aria-live="polite"
        >
          <CalloutCard :show-caret="true">
            <span class="daisy-text-sm daisy-min-w-0 daisy-flex-1">
              Create a new block from here?
            </span>
            <div class="daisy-flex daisy-gap-2 daisy-shrink-0">
              <button
                type="button"
                data-testid="book-reading-content-new-block-confirm"
                class="daisy-btn daisy-btn-primary daisy-btn-sm"
                @click="onNewBlockConfirm"
              >
                New block
              </button>
              <button
                type="button"
                data-testid="book-reading-content-new-block-cancel"
                class="daisy-btn daisy-btn-sm"
                @click="onCalloutDismiss"
              >
                Cancel
              </button>
            </div>
          </CalloutCard>
        </li>
      </template>
    </ul>
    <p
      v-else
      class="daisy-m-0 daisy-text-sm daisy-text-base-content/50"
    >
      No imported body for this section.
    </p>
  </section>
  <dialog
    class="daisy-modal"
    :class="{ 'daisy-modal-open': titleModal !== null }"
    aria-labelledby="book-reading-new-block-title-heading"
    data-testid="book-reading-new-block-title-dialog"
  >
    <div v-if="titleModal" class="daisy-modal-box">
      <h2
        id="book-reading-new-block-title-heading"
        class="daisy-text-lg daisy-font-semibold daisy-mb-2"
      >
        Name the new block
      </h2>
      <label class="daisy-form-control daisy-w-full">
        <span class="daisy-label daisy-text-sm">Title</span>
        <input
          v-model="titleModal.draft"
          type="text"
          class="daisy-input daisy-input-bordered daisy-w-full"
          data-testid="book-reading-new-block-title-input"
          :maxlength="BOOK_BLOCK_STRUCTURAL_TITLE_MAX_CHARS"
        >
      </label>
      <div class="daisy-modal-action">
        <button
          type="button"
          class="daisy-btn daisy-btn-primary"
          data-testid="book-reading-new-block-title-confirm"
          @click="onTitleModalConfirm"
        >
          Confirm
        </button>
        <button
          type="button"
          class="daisy-btn"
          data-testid="book-reading-new-block-title-cancel"
          @click="onTitleModalDismiss"
        >
          Cancel
        </button>
      </div>
    </div>
    <form method="dialog" class="daisy-modal-backdrop">
      <button type="button" @click="onTitleModalDismiss">
        close
      </button>
    </form>
  </dialog>
</template>

<script setup lang="ts">
import CalloutCard from "@/components/book-reading/CalloutCard.vue"
import { previewTextFromContentBlockRaw } from "@/lib/book-reading/contentBlockRawPreview"
import {
  BOOK_BLOCK_STRUCTURAL_TITLE_MAX_CHARS,
  defaultStructuralTitleDraft,
  structuralTitleSourceFromContentBlockRaw,
} from "@/lib/book-reading/contentBlockStructuralTitleSource"
import type { BookContentBlockFull } from "@generated/doughnut-backend-api"
import { computed, ref } from "vue"

const HOLD_THRESHOLD_MS = 500
const HOLD_MOVE_TOLERANCE_PX = 10

const props = defineProps<{
  contentBlocks: BookContentBlockFull[]
  selectedBlockTitle?: string | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  createBlockFromContent: [
    payload: { contentBlockId: number; structuralTitle?: string },
  ]
}>()

const ariaLabel = computed(() => {
  const t = props.selectedBlockTitle?.trim()
  return t
    ? `Imported content for ${t}`
    : "Imported content for selected section"
})

type HoldState = {
  contentBlockId: number
  pointerId: number
  startX: number
  startY: number
  timerId: ReturnType<typeof setTimeout>
}

const holdState = ref<HoldState | null>(null)
const calloutBlockId = ref<number | null>(null)
const titleModal = ref<{ contentBlockId: number; draft: string } | null>(null)

function cancelHold() {
  if (holdState.value) {
    clearTimeout(holdState.value.timerId)
    holdState.value = null
  }
}

function onEscape() {
  if (titleModal.value !== null) {
    onTitleModalDismiss()
    return
  }
  onCalloutDismiss()
}

function onContentBlockPointerDown(
  block: BookContentBlockFull,
  e: PointerEvent
) {
  if (props.disabled || calloutBlockId.value !== null) return
  if (titleModal.value !== null) return
  if (e.pointerType === "mouse" && e.button !== 0) return
  cancelHold()
  const timerId = setTimeout(() => {
    holdState.value = null
    calloutBlockId.value = block.id
  }, HOLD_THRESHOLD_MS)
  holdState.value = {
    contentBlockId: block.id,
    pointerId: e.pointerId,
    startX: e.clientX,
    startY: e.clientY,
    timerId,
  }
}

function onContentBlockPointerMove(
  block: BookContentBlockFull,
  e: PointerEvent
) {
  const s = holdState.value
  if (!s || s.pointerId !== e.pointerId || s.contentBlockId !== block.id) return
  const dx = e.clientX - s.startX
  const dy = e.clientY - s.startY
  if (Math.sqrt(dx * dx + dy * dy) > HOLD_MOVE_TOLERANCE_PX) {
    cancelHold()
  }
}

function onContentBlockPointerUp(block: BookContentBlockFull, e: PointerEvent) {
  const s = holdState.value
  if (!s || s.pointerId !== e.pointerId || s.contentBlockId !== block.id) return
  cancelHold()
}

function onContentBlockPointerCancel(
  block: BookContentBlockFull,
  e: PointerEvent
) {
  const s = holdState.value
  if (!s || s.pointerId !== e.pointerId || s.contentBlockId !== block.id) return
  cancelHold()
}

function onNewBlockConfirm() {
  const id = calloutBlockId.value
  if (id === null) return
  const block = props.contentBlocks.find((b) => b.id === id)
  calloutBlockId.value = null
  if (!block) return
  const { fullText, exceedsMax } = structuralTitleSourceFromContentBlockRaw(
    block.raw
  )
  if (exceedsMax) {
    titleModal.value = {
      contentBlockId: id,
      draft: defaultStructuralTitleDraft(fullText),
    }
  } else {
    emit("createBlockFromContent", { contentBlockId: id })
  }
}

function onTitleModalConfirm() {
  const m = titleModal.value
  if (m === null) return
  titleModal.value = null
  const trimmed = m.draft.trim()
  if (trimmed.length > 0) {
    emit("createBlockFromContent", {
      contentBlockId: m.contentBlockId,
      structuralTitle: trimmed,
    })
  } else {
    emit("createBlockFromContent", { contentBlockId: m.contentBlockId })
  }
}

function onTitleModalDismiss() {
  titleModal.value = null
}

function onCalloutDismiss() {
  calloutBlockId.value = null
}
</script>
