<template>
  <div id="add-comment-textbox">
    <QuillEditor
    :content="commentText"
    :options="editorOptions"
    :content-type="'html'"
    />
  </div>
  <button id="add-comment-button">Add comment</button>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { QuillEditor } from "@vueup/vue-quill";
import useLoadingApi from "../../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: { 
    noteId: { type: Number, required: true } 
  },
  emits: [],
  components: { QuillEditor },
  data() {
    return {
      commentText: "",
      editorOptions: {
        modules: {
          toolbar: false,
        },
        placeholder: "Enter comment here...",
      },
    };
  },
  methods: {},
  mounted() {},
});
</script>

<style lang="scss" scoped>
.info-button {
  width: 100%;
  align-items: center;
  justify-content: center;
  font-size: smaller;
  cursor: pointer;
  display: block;
}
</style>
