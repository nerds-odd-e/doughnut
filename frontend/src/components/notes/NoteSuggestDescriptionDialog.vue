<template>
  <h1>Suggested Description</h1>
  <form @submit.prevent.once="processForm">
    <TextArea
      v-model="suggestedDescription"
      field="suggestion"
      :errors="errorMessage"
    />
    <div class="dialog-buttons">
      <input type="submit" value="Use" class="btn btn-primary" />
    </div>
  </form>
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
        title: this.selectedNote.textContent.title,
        description: this.suggestedDescription,
        updatedAt: this.selectedNote.textContent.updatedAt,
      } as Generated.TextContent;
    },
    aiSuggestionRequest(): Generated.AiSuggestionRequest {
      return {
        prompt: `In one paragraph, tell me about ${this.selectedNote.title}`,
      };
    },
  },
  methods: {
    processForm() {
      this.storageAccessor
        .api()
        .updateTextContent(
          this.selectedNote.id,
          this.textContent,
          this.selectedNote.textContent
        )
        .then(this.popup.done);
    },
  },
  mounted() {
    this.api.ai
      .askAiSuggestions(this.aiSuggestionRequest)
      .then((res) => {
        this.suggestedDescription = res.suggestion;
      })
      .catch((er) => {
        this.errorMessage = er.message;
      });
  },
});
</script>

<style lang="sass" scoped>
.dialog-buttons
  display: flex
  column-gap: 10px
  margin: 10px 0
</style>
