<template>
  <div role="accessory">
    <ShowPicture
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
      @click="downloadAudioFile(noteAccessory.audioId!)"
      v-if="noteAccessory.audioName"
    >
      Download {{ noteAccessory.audioName }}
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { NoteAccessory } from "@/generated/backend";
import ShowPicture from "./ShowPicture.vue";

export default defineComponent({
  props: {
    noteAccessory: { type: Object as PropType<NoteAccessory>, required: true },
  },
  components: {
    ShowPicture,
  },
  methods: {
    async downloadAudioFile(audioId: number) {
      const audioUrl = `/attachments/audio/${audioId}`;
      const link = document.createElement("a");
      link.href = audioUrl;
      link.download = this.noteAccessory.audioName!;
      link.click();
    },
  },
});
</script>
