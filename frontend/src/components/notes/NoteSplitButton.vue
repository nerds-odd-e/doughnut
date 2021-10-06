<template>
  <button class="btn btn-small" title="split note" @click="showDialog()">
    <SvgSplitNote />
  </button>
</template>

<script>
import SvgSplitNote from "../svgs/SvgSplitNote.vue";
import {restPost} from "../../restful/restful";

export default {
  name: "NoteSplitButton",
  components: {
    SvgSplitNote,
  },
  props: { noteId: Number },
  methods: {
    async showDialog() {
      if (await this.$popups.confirm(`Are you sure to split this note?`)) {
        restPost(`/api/notes/${this.noteId}/split`, {}, (r) => {})
      }
    },
  },
};
</script>
