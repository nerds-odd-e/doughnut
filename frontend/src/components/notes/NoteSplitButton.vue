<template>
  <button class="btn btn-small" title="split note" @click="showDialog()">
    <SvgSplitNote />
  </button>
</template>

<script>
import SvgSplitNote from "../svgs/SvgSplitNote.vue";

export default {
  name: "NoteSplitButton",
  components: {
    SvgSplitNote,
  },
  props: { noteId: Number },
  methods: {
    async showDialog() {
    },
  },
};
</script>
