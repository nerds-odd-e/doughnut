<template>
  <span class="link-nob">
    <PopupButton :title="link.linkType">
      <template #button_face>
        <SvgLinkTypeIcon
          :link-type="link.linkType"
          :inverse-icon="inverseIcon"
        />
      </template>
      <template #dialog_body="{ doneHandler }">
        <LinkNobDialog
          v-bind="{ link, inverseIcon, colors }"
          @done="
            doneHandler($event);
            $emit('noteRealmUpdated', $event);
          "
          @link-deleted="
            doneHandler($event);
            $emit('noteRealmUpdated', $event);
          "
        />
      </template>
    </PopupButton>
  </span>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";
import LinkNobDialog from "./LinkNobDialog.vue";

export default defineComponent({
  props: {
    link: {
      type: Object as PropType<Generated.Link>,
      required: true,
    },
    inverseIcon: Boolean,
    colors: Object,
  },
  emits: ["noteRealmUpdated"],
  components: { SvgLinkTypeIcon, PopupButton, LinkNobDialog },
});
</script>
