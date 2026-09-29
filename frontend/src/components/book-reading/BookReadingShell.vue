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
    v-bind="$attrs"
    v-model:opened="bookLayoutOpened"
    :panel-id="bookReadingBookLayoutPanelId"
    :is-md-or-larger="isMdOrLarger"
    :blocks="bookBlocks"
    :current-block-id="currentBlockId"
    :selected-block-id="selectedBlockId"
    :disposition-for-block="bookReading.dispositionForBlock"
    @block-click="applyBookBlockSelection"
    @change-mark="bookReading.submitReadingDisposition"
    @clear-mark="bookReading.clearReadingDisposition"
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
          <ReadingControlPanel
            v-if="blockAwaitingConfirmation"
            :selected-block-title="blockAwaitingConfirmation.title"
            :snap-animation-key="snapAnimationKey"
            :anchor-top-px="readingPanelAnchorTopPx"
            @mark-as-read="() => markSelectedBlockDisposition('READ')"
            @mark-as-skimmed="() => markSelectedBlockDisposition('SKIMMED')"
            @mark-as-skipped="() => markSelectedBlockDisposition('SKIPPED')"
          />
          <slot name="pane-end" />
        </div>
      </div>
    </main>
    <main
      v-else
      :ref="setMainPane"
      class="flex flex-1 min-h-0 min-w-0 flex-col relative"
    >
      <slot />
      <ReadingControlPanel
        v-if="blockAwaitingConfirmation"
        :selected-block-title="blockAwaitingConfirmation.title"
        :anchor-top-px="readingPanelAnchorTopPx"
        @mark-as-read="() => markSelectedBlockDisposition('READ')"
        @mark-as-skimmed="() => markSelectedBlockDisposition('SKIMMED')"
        @mark-as-skipped="() => markSelectedBlockDisposition('SKIPPED')"
      />
    </main>
  </BookReadingBookLayout>
</template>

<script setup lang="ts">
import BookLayoutToggleButton from "@/components/book-reading/BookLayoutToggleButton.vue"
import BookReadingBookLayout from "@/components/book-reading/BookReadingBookLayout.vue"
import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type { BookReadingSession } from "@/composables/useBookReadingSession"
import { useSidebarDrawer } from "@/composables/useSidebarDrawer"

/**
 * The reading view both formats share, bound to one reading session. `format` keeps each
 * format's current markup: PDF's live announcement, load error, and framed pane; EPUB's
 * title test id. Listeners not declared here go to the book layout.
 */
defineOptions({ inheritAttrs: false })

const props = defineProps<{
  session: BookReadingSession
  format: "pdf" | "epub"
  bookName: string
  loadError?: string | null
  snapAnimationKey?: number
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
