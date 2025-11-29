<template>
  <div class="note-under-question daisy-bg-base-200 daisy-border-base-300">
    <div class="note-label daisy-bg-base-200 daisy-text-base-content">Note under question</div>
    <div class="breadcrumb-wrapper">
      <Breadcrumb v-bind="{ noteTopology, includingSelf: true }" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/backend"
import Breadcrumb from "@/components/toolbars/Breadcrumb.vue"

defineProps({
  noteTopology: {
    type: Object as PropType<NoteTopology>,
    required: true,
  },
})
</script>

<style lang="sass" scoped>
.note-under-question
  width: 100%
  border-radius: 5px
  padding: 8px 12px
  padding-top: 24px
  margin-top: 15px
  border: 2px solid
  position: relative

  .note-label
    position: absolute
    top: -12px
    left: 10px
    padding: 0 10px
    font-weight: 500
    font-size: 0.9rem
    border-radius: 3px
    z-index: 1

  .breadcrumb-wrapper
    width: 100%
    overflow-x: auto
    :deep(.daisy-breadcrumbs)
      max-width: none !important
      width: max-content
</style>
