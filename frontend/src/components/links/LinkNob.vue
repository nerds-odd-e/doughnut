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

<script lang="ts">
import { defineComponent } from "vue";
import usePopups from "../commons/Popups/usePopup";
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";
import LinkNobDialog from "./LinkNobDialog.vue";

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: { link: Object, inverseIcon: Boolean, colors: Object },
  components: { SvgLinkTypeIcon },
  methods: {
    showDialog() {
      this.popups.dialog(LinkNobDialog, this.$props)
    },
  },
});
</script>
