<template>
  <div role="accessory">
    <ShowImage
      v-if="noteAccessory.imageWithMask"
      class="text-center"
      v-bind="noteAccessory.imageWithMask"
      :opacity="0.2"
    />
    <div v-if="!!noteAccessory.url">
      <label id="note-url" v-text="'Url:'" />
      <a aria-labelledby="note-url" :href="noteAccessory.url" target="_blank">{{
        noteAccessory.url
      }}</a>
    </div>
    <button
      class="btn btn-sm download-btn"
      @click="downloadAudioFile(noteAccessory.audioAttachment)"
      v-if="noteAccessory.audioAttachment"
    >
      Download {{ noteAccessory.audioAttachment.name }}
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Audio, NoteAccessory } from "@/generated/backend";
import ShowImage from "./ShowImage.vue";

export default defineComponent({
  props: {
    noteAccessory: { type: Object as PropType<NoteAccessory>, required: true },
  },
  components: {
    ShowImage,
  },
  methods: {
    async downloadAudioFile(audioAttachment: Audio) {
      const audioUrl = `/attachments/audio/${audioAttachment.id}`;
      const link = document.createElement("a");
      link.href = audioUrl;
      link.download = audioAttachment.name!;
      link.click();
    },
  },
});
</script>
