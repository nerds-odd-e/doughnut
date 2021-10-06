<template>
  <button class="btn btn-small" title="split note" @click="showDialog()">
    <SvgEdit />
  </button>
</template>

<script>
import SvgEdit from "../svgs/SvgEdit.vue";

export default {
  name: "NoteSplitButton",
  components: {
    SvgEdit,
  },
  props: { noteId: Number },
  methods: {
    async showDialog() {
    },
  },
};
</script>
