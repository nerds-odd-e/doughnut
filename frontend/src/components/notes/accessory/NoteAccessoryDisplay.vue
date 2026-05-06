<template>
  <div role="accessory">
    <ShowImage
      v-if="noteAccessory.imageWithMask"
      class="daisy-text-center"
      v-bind="noteAccessory.imageWithMask"
      :opacity="0.2"
    />
  </div>
</template>

<script setup lang="ts">
import type { NoteAccessory } from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import ShowImage from "./ShowImage.vue"

defineProps({
  noteAccessory: { type: Object as PropType<NoteAccessory>, required: true },
})
</script>
