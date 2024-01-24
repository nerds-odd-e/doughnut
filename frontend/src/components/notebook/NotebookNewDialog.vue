<template>
  <form @submit.prevent.once="processForm">
    <NoteFormTopicOnly v-model="noteFormData.topic" :errors="errors" />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { PropType } from "vue";
import NoteFormTopicOnly from "../notes/NoteFormTopicOnly.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default {
  setup() {
    return useLoadingApi();
  },
  props: { circle: { type: Object as PropType<Generated.Circle> } },
  components: {
    NoteFormTopicOnly,
  },
  data() {
    return {
      noteFormData: { topic: "" } as Generated.TextContent,
      errors: {},
    };
  },
  methods: {
    processForm() {
      this.api.notebookMethods
        .createNotebook(this.circle, this.noteFormData)
        .then((res) =>
          this.$router.push({
            name: "noteShow",
            params: { noteId: res.noteId },
          }),
        )
        .catch((err) => (this.errors = err));
    },
  },
};
</script>
