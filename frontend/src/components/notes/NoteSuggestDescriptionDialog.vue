<template>
  <LoadingPage v-bind="{ contentExists: true }">
    <h1>Suggested Description</h1>
    <form @submit.prevent.once="processForm">
      <textarea
        :class="`area-control form-control`"
        :autofocus="true"
        v-model="suggestedDescription"
        role="suggestdescription"
        autocomplete="off"
        autocapitalize="off"
        rows="8"
        ref="input"
      />
      <input type="submit" value="Use" class="btn btn-primary" />
      <button
        type="button"
        value="Copy to clipboard"
        class="btn btn-primary"
        @click="copyText()"
      >
        Copy to clipboard
      </button>
    </form>
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import { StorageAccessor } from "../../store/createNoteStorage";

function selectAllTextInTextAreaElement(element: HTMLTextAreaElement) {
  element.select();
  element.setSelectionRange(0, 99999);
  document.execCommand("copy");
}

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
    copyText() {
      const element = this.$refs.input as InstanceType<
        typeof HTMLTextAreaElement
      >;
      if (!navigator.clipboard) {
        selectAllTextInTextAreaElement(element);
      } else {
        navigator.clipboard.writeText(element.value);
      }
    },
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
