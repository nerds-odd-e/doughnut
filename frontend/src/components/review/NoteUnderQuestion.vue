<template>
  <div class="note-under-question">
    <div class="note-label">Note under question</div>
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
  background-color: #f9f9f9
  border: 2px solid #e0e0e0
  position: relative

  .note-label
    position: absolute
    top: -12px
    left: 10px
    padding: 0 10px
    font-weight: 500
    color: #666
    font-size: 0.9rem
    background-color: #f9f9f9
    border-radius: 3px

  .breadcrumb-wrapper
    width: 100%
    overflow-x: auto
    :deep(.daisy-breadcrumbs)
      max-width: none !important
      width: max-content
</style>
