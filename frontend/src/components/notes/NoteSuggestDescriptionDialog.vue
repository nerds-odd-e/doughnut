<template>
  <h1>Suggested Description</h1>
  <TextArea
    v-model="textToComplete"
    field="suggestion"
    :errors="errorMessage"
  />
  <div class="dialog-buttons">
    <button
      class="btn btn-primary"
      @click="appendToDescription"
      :disabled="!contentReady"
    >
      Use
    </button>
    <button
      class="btn btn-secondary"
      @click="askForSuggestion"
      :disabled="!contentReady || lastCompletionResult === textToComplete"
    >
      Ask again
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import TextArea from "../form/TextArea.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import { StorageAccessor } from "../../store/createNoteStorage";
import asPopup from "../commons/Popups/asPopup";

export default defineComponent({
  setup() {
    return { ...useLoadingApi(), ...asPopup() };
  },
  props: {
    selectedNote: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  data() {
    return {
      textToComplete: `Tell me about "${this.selectedNote.title}"` as string,
      errorMessage: undefined as string | undefined,
      contentReady: false as boolean,
      lastCompletionResult: undefined as undefined | string,
      tries: 0 as number,
    };
  },
  computed: {
    textContent() {
      return {
        title: this.selectedNote.title,
        description: this.textToComplete,
      } as Generated.TextContent;
    },
    aiSuggestionRequest(): Generated.AiSuggestionRequest {
      return {
        prompt: this.textToComplete,
      };
    },
  },
  methods: {
    appendToDescription() {
      this.storageAccessor
        .api(this.$router)
        .updateTextContent(
          this.selectedNote.id,
          this.textContent,
          this.selectedNote.textContent
        )
        .then(this.popup.done);
    },
    askForSuggestion() {
      this.contentReady = false;
      this.api.ai
        .askAiSuggestions(this.aiSuggestionRequest)
        .then((res: Generated.AiSuggestion) => {
          this.textToComplete = res.suggestion;
          this.lastCompletionResult = res.suggestion;
          if (res.finishReason !== "length") {
            this.contentReady = true;
          }
          this.tries += 1;
        })
        .catch((er) => {
          this.errorMessage = er.message;
          this.contentReady = true;
        });
    },
  },
  watch: {
    tries() {
      if (!this.contentReady) {
        this.askForSuggestion();
      }
    },
  },
  mounted() {
    this.askForSuggestion();
  },
});
</script>

<style lang="sass" scoped>
.dialog-buttons
  display: flex
  column-gap: 10px
  margin: 10px 0
</style>
