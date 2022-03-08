<template>
  <button
    class="dropdown-item"
    title="Edit review settings"
    @click="showDialog()"
  >
    <SvgReviewSetting />
    <slot />
  </button>
</template>
<script>
import ReviewSettingEditDialog from "./ReviewSettingEditDialog.vue";
import SvgReviewSetting from "../svgs/SvgReviewSetting.vue";

export default {
  components: { SvgReviewSetting },
  props: { noteId: [String, Number], oldTitle: String },
  methods: {
    async showDialog() {
      await this.$popups.dialog(ReviewSettingEditDialog, {
        noteId: this.noteId,
        title: this.oldTitle,
      })
    },
  },
};
</script>
