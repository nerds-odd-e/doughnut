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
          v-for="block in bookBlockRows"
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
          :style="{ paddingLeft: `${block.depth * 0.75}rem` }"
          @click="emit('blockClick', block)"
        >
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
        </button>
      </div>
    </aside>
    <slot />
  </div>
</template>

<script setup lang="ts">
import type { BookBlockReadingDisposition } from "@/lib/book-reading/readBlockIdsFromRecords"
import type { BookBlockFull } from "@generated/doughnut-backend-api"
import { ref, watch } from "vue"

export type BookReadingBookLayoutBlockRow = Pick<
  BookBlockFull,
  "id" | "title" | "depth" | "allBboxes"
>

const opened = defineModel<boolean>("opened", { required: true })

const props = defineProps<{
  panelId: string
  isMdOrLarger: boolean
  bookBlockRows: BookReadingBookLayoutBlockRow[]
  currentBlockId: number | null
  selectedBlockId: number | null
  dispositionForBlock: (
    blockId: number
  ) => BookBlockReadingDisposition | undefined
}>()

const emit = defineEmits<{
  blockClick: [block: BookReadingBookLayoutBlockRow]
}>()

const asideRef = ref<HTMLElement | null>(null)

function closeOverlay() {
  opened.value = false
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
</script>

<style scoped>
aside {
  max-height: 100%;
}

.book-reading-book-block {
  @apply daisy-w-full daisy-min-h-10 daisy-text-left daisy-rounded-md;
  @apply daisy-border-0 daisy-border-solid daisy-border-l-4 daisy-border-transparent;
  @apply daisy-py-2 daisy-pr-2 daisy-pl-2 daisy-text-sm daisy-leading-snug daisy-font-normal;
  @apply daisy-transition-colors daisy-duration-150;
  @apply hover:daisy-bg-base-300/55;
  @apply focus:daisy-outline-none focus-visible:daisy-ring-2 focus-visible:daisy-ring-primary/50;
  @apply focus-visible:daisy-ring-offset-2 focus-visible:daisy-ring-offset-base-200;
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
