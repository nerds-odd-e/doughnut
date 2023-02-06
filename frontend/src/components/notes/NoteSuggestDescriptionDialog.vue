<template>
  <LoadingPage v-bind="{ contentExists: true }">
    <h1>Suggested Description</h1>
    <form @submit.prevent.once="processForm">
      <TextArea v-model="suggestedDescription" />
      <div class="dialog-buttons">
        <input type="submit" value="Use" class="btn btn-primary" />
      </div>
    </form>
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import TextArea from "../form/TextArea.vue";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    selectedNote: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    LoadingPage,
  },
  data() {
    return {
      suggestedDescription: "Placeholder",
    };
  },
  computed: {
    textContent() {
      return {
        title: this.selectedNote.textContent.title,
        description: this.suggestedDescription,
        updatedAt: this.selectedNote.textContent.updatedAt,
      };
    },
  },
  emits: ["done"],
  methods: {
    processForm() {
      this.storageAccessor
        .api()
        .updateTextContent(
          this.selectedNote.id,
          this.textContent,
          this.selectedNote.textContent
        )
        .then(() => {
          this.$emit("done");
        });
    },
  },
});
</script>

<style lang="sass" scoped>
.dialog-buttons
  display: flex
  column-gap: 10px
  margin: 10px 0
</style>
