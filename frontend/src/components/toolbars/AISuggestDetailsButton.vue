<template>
  <a
    :title="'suggest details'"
    class="btn btn-sm"
    role="button"
    @click="suggestDetails(selectedNote.details)"
  >
    <SvgRobot />
  </a>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import SvgRobot from "../svgs/SvgRobot.vue";

export default defineComponent({
  setup() {
    return {
      ...useLoadingApi(),
    };
  },
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    selectedNote: {
      type: Object as PropType<Generated.Note>,
      required: true,
    },
  },
  components: {
    SvgRobot,
  },
  data() {
    return {
      isUnmounted: false,
    };
  },
  methods: {
    async suggestDetails(prev?: string) {
      const prompt = `Please provide the details for the note titled: ${this.selectedNote.topic}`;
      await this.api.ai.keepAskingAICompletionUntilStop(
        prompt,
        this.selectedNote.id,
        prev,
        (moreCompleteContent) => {
          this.storageAccessor.api(this.$router).updateTextContent(
            this.selectedNote.id,
            {
              topic: this.selectedNote.topic,
              details: moreCompleteContent,
            },
            {
              topic: this.selectedNote.topic,
              details: this.selectedNote.details,
            },
          );
          return !this.isUnmounted;
        },
      );
    },
  },
  unmounted() {
    this.isUnmounted = true;
  },
});
</script>
