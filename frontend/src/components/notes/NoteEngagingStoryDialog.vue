<template>
  <LoadingPage v-bind="{ contentExists: true }">
    <h1>Have fun reading this engaging story</h1>
    <form @submit.prevent.once="processForm">
      <TextArea v-model="engagingStory" />
      <div class="dialog-buttons">
        <input type="submit" value="Close" class="btn btn-primary" />
      </div>
    </form>
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import type { StorageAccessor } from "@/store/createNoteStorage";

export default defineComponent({
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
      engagingStory: "Coming soon.",
    };
  },
  computed: {
    textContent() {
      return {
        title: this.selectedNote.textContent.title,
        description: this.engagingStory,
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
