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
<script lang="ts">
import { defineComponent } from "vue";
import ReviewSettingEditDialog from "./ReviewSettingEditDialog.vue";
import SvgReviewSetting from "../svgs/SvgReviewSetting.vue";
import usePopups from "../commons/Popups/usePopup";


export default defineComponent({
  setup() {
    return usePopups();
  },
  components: { SvgReviewSetting },
  props: { noteId: Number, oldTitle: String },
  methods: {
    async showDialog() {
      await this.popups.dialog(ReviewSettingEditDialog, {
        noteId: this.noteId,
        title: this.oldTitle,
      })
    },
  },
});
</script>
