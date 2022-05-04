<template>
  <TextInput field="comment" v-focus v-model="content" />
  <button @click="createComment">Submit</button>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import TextInput from "../form/TextInput.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
  },
  data() {
    return {
      content: "",
    };
  },
  components: {
    TextInput,
  },
  emits: ["done"],
  methods: {
    async createComment() {
      await this.api.comments.createNoteComment(this.noteId, {
        description: this.content,
      });
      this.$router.push({ name: "noteShow", params: { noteId: this.noteId } });
    },
  },
});
</script>
