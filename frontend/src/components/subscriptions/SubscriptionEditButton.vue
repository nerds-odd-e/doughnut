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

<script>
import SvgEdit from "../svgs/SvgEdit.vue";
import SubscriptionEditDialog from "./SubscriptionEditDialog.vue";

export default {
  props: { subscription: Object },
  components: { SvgEdit },

  methods: {
    showDialog() {
      this.$popups.dialog(SubscriptionEditDialog, this.$props)
    },
  },
};
</script>
