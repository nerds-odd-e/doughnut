<template>
  <div
    v-if="format === 'pdf'"
    role="status"
    aria-live="polite"
    aria-atomic="true"
    data-testid="book-reading-current-block-live"
    class="sr-only"
  >
    {{ currentBlockLiveText }}
  </div>
  <GlobalBar>
    <BookLayoutToggleButton
      v-model:opened="bookLayoutOpened"
      :panel-id="bookReadingBookLayoutPanelId"
    />
    <router-link
      :to="{ name: 'notebookPage', params: { notebookId: notebookId } }"
      class="daisy-btn daisy-btn-sm daisy-btn-ghost shrink-0 no-underline"
    >
      Notebook
    </router-link>
    <span
      class="truncate text-sm font-medium min-w-0 ml-1"
      :data-testid="
        format === 'epub' ? 'book-reading-epub-global-bar-title' : undefined
      "
      :title="bookName"
    >
      {{ bookName }}
    </span>
    <slot name="bar-end">
      <span class="ml-auto shrink-0" aria-hidden="true" />
    </slot>
  </GlobalBar>
  <BookReadingBookLayout
    v-model:opened="bookLayoutOpened"
    :panel-id="bookReadingBookLayoutPanelId"
    :is-md-or-larger="isMdOrLarger"
    :blocks="bookBlocks"
    :current-block-id="currentBlockId"
    :selected-block-id="selectedBlockId"
    :can-undo-depth-change="reorganize?.canUndoDepthChange.value"
    :disposition-for-block="bookReading.dispositionForBlock"
    @block-click="applyBookBlockSelection"
    @change-mark="bookReading.submitReadingDisposition"
    @clear-mark="bookReading.clearReadingDisposition"
    v-on="reorganize?.layoutListeners ?? {}"
  >
    <main
      v-if="format === 'pdf'"
      class="flex flex-1 min-h-0 min-w-0 flex-col"
    >
      <div
        v-if="loadError"
        class="daisy-alert daisy-alert-error mb-2 mx-2 mt-2"
        data-testid="book-reading-pdf-viewer-load-error"
      >
        {{ loadError }}
      </div>
      <div
        v-else
        class="flex min-h-0 min-w-0 flex-1 flex-col"
      >
        <div
          :ref="setMainPane"
          class="relative min-h-0 min-w-0 flex-1"
        >
          <slot />
          <ReadingOverlayDock @wheel="emit('overlayWheel', $event)">
            <ReadingControlPanel
              v-if="blockAwaitingConfirmation"
              :selected-block-title="blockAwaitingConfirmation.title"
              :anchor-top-px="readingPanelAnchorTopPx"
              @mark-as-read="() => markSelectedBlockDisposition('READ')"
              @mark-as-skimmed="() => markSelectedBlockDisposition('SKIMMED')"
              @mark-as-skipped="() => markSelectedBlockDisposition('SKIPPED')"
            />
            <CurrentBlockNavigationBar
              v-if="reorganize?.currentBlockForNavBar.value"
              :current-block-title="reorganize.currentBlockForNavBar.value.title"
              @read-from-here="reorganize.readFromHere"
              @back-to-selected="reorganize.backToSelected"
            />
          </ReadingOverlayDock>
        </div>
      </div>
    </main>
    <main
      v-else
      :ref="setMainPane"
      class="flex flex-1 min-h-0 min-w-0 flex-col relative"
    >
      <slot />
      <ReadingOverlayDock>
        <ReadingControlPanel
          v-if="blockAwaitingConfirmation"
          :selected-block-title="blockAwaitingConfirmation.title"
          :anchor-top-px="readingPanelAnchorTopPx"
          @mark-as-read="() => markSelectedBlockDisposition('READ')"
          @mark-as-skimmed="() => markSelectedBlockDisposition('SKIMMED')"
          @mark-as-skipped="() => markSelectedBlockDisposition('SKIPPED')"
        />
      </ReadingOverlayDock>
    </main>
  </BookReadingBookLayout>
  <BookLayoutReorganizePreviewDialog
    v-if="reorganize"
    :open="reorganize.aiSuggestion.value !== null"
    :preview-rows="reorganize.aiPreviewRows.value"
    @confirm="reorganize.confirmAiReorganize"
    @cancel="reorganize.dismissAiReorganizePreview"
  />
</template>

<script setup lang="ts">
import BookLayoutReorganizePreviewDialog from "@/components/book-reading/BookLayoutReorganizePreviewDialog.vue"
import BookLayoutToggleButton from "@/components/book-reading/BookLayoutToggleButton.vue"
import BookReadingBookLayout from "@/components/book-reading/BookReadingBookLayout.vue"
import CurrentBlockNavigationBar from "@/components/book-reading/CurrentBlockNavigationBar.vue"
import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import ReadingOverlayDock from "@/components/book-reading/ReadingOverlayDock.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type { BookReadingSession } from "@/composables/useBookReadingSession"
import { useSidebarDrawer } from "@/composables/useSidebarDrawer"

/**
 * The reading view both formats share, bound to one reading session. `format` keeps each
 * format's current markup: PDF's live announcement, load error, and framed pane; EPUB's
 * title test id. Reorganizing and the "Now reading" bar are wired only when the session
 * allows reorganizing.
 */

const props = defineProps<{
  session: BookReadingSession
  format: "pdf" | "epub"
  bookName: string
  loadError?: string | null
}>()

const emit = defineEmits<{
  /** A wheel over the PDF pane's Reading Control Panel or "Now reading" bar. */
  overlayWheel: [event: WheelEvent]
}>()

const bookReadingBookLayoutPanelId = "book-reading-book-layout-panel"

const {
  notebookId,
  bookBlocks,
  bookReading,
  selectedBlockId,
  currentBlockId,
  currentBlockLiveText,
  blockAwaitingConfirmation,
  applyBookBlockSelection,
  markSelectedBlockDisposition,
  readingPanelAnchorTopPx,
  reorganize,
} = props.session

function setMainPane(el: unknown) {
  props.session.mainPane.value = el as HTMLElement | null
}

/**
 * Decided synchronously so the book layout aside is in its final open/closed state before
 * the viewer mounts; otherwise a late EPUB resize can redisplay the wrong section.
 */
const { opened: bookLayoutOpened, isMdOrLarger } = useSidebarDrawer()
</script>
