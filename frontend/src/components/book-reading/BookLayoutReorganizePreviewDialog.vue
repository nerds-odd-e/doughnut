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
        class="daisy-text-lg daisy-font-semibold"
      >
        Reorganize layout (preview)
      </h2>
      <div
        class="daisy-max-h-[min(24rem,50vh)] daisy-overflow-y-auto daisy-py-2"
      >
        <div
          v-for="row in previewRows"
          :key="row.block.id"
          data-testid="book-layout-reorganize-preview-row"
          class="daisy-rounded daisy-py-1.5 daisy-pr-2 daisy-text-sm daisy-leading-snug"
          :class="{
            'daisy-bg-warning/15': row.depthChanged,
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
