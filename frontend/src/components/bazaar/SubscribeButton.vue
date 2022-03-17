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

<script lang="ts">
import { defineComponent } from "vue";
import usePopups from "../commons/Popups/usePopup";
import SvgAdd from "../svgs/SvgAdd.vue";
import SubscribeDialog from "./SubscribeDialog.vue";

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: { notebook: Object, user: Boolean },
  components: { SvgAdd },
  methods: {
    showDialog() {
      this.popups.dialog(SubscribeDialog, this.$props);
    },
  },
});
</script>
