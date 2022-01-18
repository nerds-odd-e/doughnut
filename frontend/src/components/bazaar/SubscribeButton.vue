<template>
      <button
        class="btn btn-sm"
        role="button"
        @click="showDialog"
        title="Add to my learning"
      >
        <SvgAdd />
      </button>
</template>

<script>
import SvgAdd from "../svgs/SvgAdd.vue";
import SubscribeDialog from "./SubscribeDialog.vue";

export default {
  props: { notebook: Object, user: Object },
  components: { SvgAdd },
  methods: {
    showDialog() {
      this.$popups.dialog(SubscribeDialog, this.$props)
    },
  },
};
</script>
