<template>
  <h1>Suggested Description</h1>
  <TextArea
    v-model="suggestedDescription"
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
      suggestedDescription: undefined as string | undefined,
      errorMessage: undefined as string | undefined,
    };
  },
  computed: {
    textContent() {
      return {
        title: this.selectedNote.title,
        description: this.suggestedDescription,
      } as Generated.TextContent;
    },
    aiSuggestionRequest(): Generated.AiSuggestionRequest {
      return {
        prompt: `Tell me about "${this.selectedNote.title}"`,
      };
    },
    contentReady() {
      return this.suggestedDescription !== undefined;
    },
  },
  methods: {
    appendToDescription() {
      this.storageAccessor
        .api()
        .updateTextContent(
          this.selectedNote.id,
          this.textContent,
          this.selectedNote.textContent
        )
        .then(this.popup.done);
    },
    askForSuggestion(prompt: Generated.AiSuggestionRequest) {
      this.api.ai
        .askAiSuggestions(prompt)
        .then((res: Generated.AiSuggestion) => {
          this.suggestedDescription = res.suggestion;
          if (res.finishReason === "length") {
            this.askForSuggestion({ prompt: res.suggestion });
          }
        })
        .catch((er) => {
          this.errorMessage = er.message;
        });
    },
  },
  mounted() {
    this.askForSuggestion(this.aiSuggestionRequest);
  },
});
</script>

<style lang="sass" scoped>
.dialog-buttons
  display: flex
  column-gap: 10px
  margin: 10px 0
</style>
