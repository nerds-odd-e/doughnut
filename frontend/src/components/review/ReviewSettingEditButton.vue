<template>
  <button class="dropdown-item" title="edit review setting" @click="showDialog()">
    <SvgReviewSetting/>
    <slot />
  </button>

</template>
<script>
import ReviewSettingEditDialog from "./ReviewSettingEditDialog.vue"
import SvgReviewSetting from "../svgs/SvgReviewSetting.vue"

export default {
  components: { SvgReviewSetting },
  props: {noteId: Number, oldTitle: String},
  emits: ['updated'],
  methods: {
    async showDialog() {
      if(await this.$popups.dialog(
        ReviewSettingEditDialog,
        {noteId: this.noteId, title: this.oldTitle}
      )) {
        this.$emit('updated')
      }
    }

  },
}
</script>
