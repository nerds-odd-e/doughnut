<template>
  <div id="add-comment-textbox">
    <QuillEditor
      :content="commentText"
      :options="editorOptions"
      :content-type="'html'"
    />
  </div>
  <button id="add-comment-button">Add comment</button>

  <!-- <div
    id="comments"
    v-if="comments?.length !== 0"
  >
    <Comment
      v-for="comment in comments"
      :key="comment.note.id"
      :text="comment.text"
    />
  </div> -->
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import { QuillEditor } from "@vueup/vue-quill";
import { StorageAccessor } from "@/store/createNoteStorage";
import useLoadingApi from "../../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
    // comments: { type: Array as PropType<Array<Generated.Comment>>, required: false },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
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
  methods: {
    fetchData: () => {
      // Call GetCommentsByNoteId(...)
      // Store in data.comments
    },
  },
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
