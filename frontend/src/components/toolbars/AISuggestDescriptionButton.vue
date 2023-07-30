<template>
  <a
    :title="'suggest description'"
    class="btn btn-sm"
    role="button"
    @click="suggestDescription(selectedNote.description)"
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
    async suggestDescription(prev?: string) {
      const prompt = `Please provide the description for the note titled: ${this.selectedNote.title}`;
      await this.api.ai.keepAskingAICompletionUntilStop(
        prompt,
        this.selectedNote.id,
        prev,
        (moreCompleteContent) => {
          this.storageAccessor.api(this.$router).updateTextContent(
            this.selectedNote.id,
            {
              title: this.selectedNote.title,
              description: moreCompleteContent,
            },
            this.selectedNote.textContent,
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
