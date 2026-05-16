<template>
  <dialog
    class="daisy-modal"
    :class="{ 'daisy-modal-open': open }"
    aria-labelledby="book-layout-reorganize-preview-title"
    data-testid="book-layout-reorganize-preview-dialog"
  >
    <div class="daisy-modal-box">
      <h2
        id="book-layout-reorganize-preview-title"
        class="text-lg font-semibold"
      >
        Reorganize layout (preview)
      </h2>
      <div
        class="max-h-[min(24rem,50vh)] overflow-y-auto py-2"
      >
        <div
          v-for="row in previewRows"
          :key="row.block.id"
          data-testid="book-layout-reorganize-preview-row"
          class="rounded py-1.5 pr-2 text-sm leading-snug"
          :class="{
            'bg-warning/15': row.depthChanged,
          }"
          :data-suggested-depth="row.suggestedDepth"
          :data-depth-changed="row.depthChanged ? 'true' : undefined"
          :style="{
            paddingInlineStart: `${0.75 * row.suggestedDepth}rem`,
          }"
        >
          {{ row.block.title }}
        </div>
      </div>
      <div class="daisy-modal-action">
        <button
          type="button"
          class="daisy-btn daisy-btn-primary"
          data-testid="book-layout-reorganize-preview-confirm"
          @click="emit('confirm')"
        >
          Confirm
        </button>
        <button
          type="button"
          class="daisy-btn"
          data-testid="book-layout-reorganize-preview-cancel"
          @click="emit('cancel')"
        >
          Cancel
        </button>
      </div>
    </div>
    <form method="dialog" class="daisy-modal-backdrop">
      <button type="button" @click="emit('cancel')">close</button>
    </form>
  </dialog>
</template>

<script setup lang="ts">
defineProps<{
  open: boolean
  previewRows: Array<{
    block: { id: number; title: string }
    suggestedDepth: number
    depthChanged: boolean
  }>
}>()

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()
</script>
