<template>
      <span class="link-nob">
        <a role="button" @click="showDialog" :title="link.linkTypeLabel">
          <SvgLinkTypeIcon
            :linkTypeId="link.typeId"
            :inverseIcon="inverseIcon"
          />
        </a>
      </span>
</template>

<script>
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";
import LinkNobDialog from "./LinkNobDialog.vue";

export default {
  props: { link: Object, inverseIcon: Boolean, colors: Object },
  components: { SvgLinkTypeIcon },
  methods: {
    showDialog() {
      this.$popups.dialog(LinkNobDialog, this.$props)
    },
  },
};
</script>
