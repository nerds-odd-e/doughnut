<script>
import NoteCommentForm from "../comment/NoteCommentForm.vue";

export default {
  name: "NoteAddCommentButton",
  props: { noteId: [String, Number], linkid: [String, Number] },
  components: { NoteCommentForm },
  data() {
    return { noteAddCommentComponentName: null };
  },
  methods: {
    toggleCommentForm() {
      if (!this.noteAddCommentComponentName) {
        this.noteAddCommentComponentName = "NoteCommentForm";
      } else {
        this.noteAddCommentComponentName = null;
      }
    },
  },
  watch: {
    noteId() {
      this.noteAddCommentComponentName = null;
    },
    linkid() {
      this.noteAddCommentComStatisticsponentName = null;
    },
  }
};
</script>

<template>
  <a
    @click="toggleCommentForm()"
    class="btn btn-sm"
    role="button"
  >
    Add Comment
  </a>
  <div>
    <component 
      :is="noteAddCommentComponentName"
      :noteId="noteId"
      :linkid="linkid"
    ></component>
  </div>
</template>
