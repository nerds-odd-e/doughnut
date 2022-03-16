<template>
      <button
        class="btn btn-sm"
        role="button"
        @click="showDialog"
        title="Edit subscription"
      >
        <SvgEdit />
      </button>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import usePopups from "../commons/usePopup";
import SvgEdit from "../svgs/SvgEdit.vue";
import SubscriptionEditDialog from "./SubscriptionEditDialog.vue";

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: { subscription: Object },
  components: { SvgEdit },

  methods: {
    showDialog() {
      this.popups.dialog(SubscriptionEditDialog, this.$props)
    },
  },
});
</script>
