<template>
  <div role="accessory">
    <ShowImage
      v-if="noteAccessory.imageWithMask"
      class="daisy-text-center"
      v-bind="noteAccessory.imageWithMask"
      :opacity="0.2"
    />
    <div v-if="!!noteAccessory.url">
      <label id="note-url" v-text="'Url:'" />
      <a aria-labelledby="note-url" :href="noteAccessory.url" target="_blank">{{
        noteAccessory.url
      }}</a>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { NoteAccessory } from "@generated/backend"
import type { PropType } from "vue"
import ShowImage from "./ShowImage.vue"

defineProps({
  noteAccessory: { type: Object as PropType<NoteAccessory>, required: true },
})
</script>
