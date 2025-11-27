<template>
  <fieldset class="note-under-question">
    <legend>Note under question</legend>
    <div class="breadcrumb-container">
      <Breadcrumb v-bind="{ noteTopology, includingSelf: true }" />
    </div>
  </fieldset>
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
  border: 1px solid #ccc
  width: 100%
  border-radius: 5px
  padding: 8px 12px
  margin-top: 15px
  background-color: #f9f9f9
  border: 2px solid #e0e0e0
  position: relative

  legend
    padding: 0 10px
    font-weight: 500
    color: #666
    font-size: 0.9rem
    background-color: #f9f9f9
    border-radius: 3px
    position: absolute
    top: -12px
    left: 10px
    width: auto

  .breadcrumb-container
    max-width: 100%
    overflow-x: auto
    overflow-y: hidden
    -webkit-overflow-scrolling: touch

    :deep(.daisy-breadcrumbs)
      white-space: nowrap

      ul
        white-space: nowrap
        display: inline-flex
</style>
