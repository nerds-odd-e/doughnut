<template>
      <span class="link-nob">
        <PopupButton
          :title="link.linkTypeLabel"
        >
          <template v-slot:button_face>
            <SvgLinkTypeIcon
              :linkTypeId="link.typeId"
              :inverseIcon="inverseIcon"
            />
          </template>
          <template #dialog_body="{doneHandler}">
            <LinkNobDialog v-bind="{link, inverseIcon, colors}" @done="doneHandler($event)"/>
          </template>
        </PopupButton>
      </span>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";
import LinkNobDialog from "./LinkNobDialog.vue";

export default defineComponent({
  props: { link: Object, inverseIcon: Boolean, colors: Object },
  components: { SvgLinkTypeIcon, PopupButton, LinkNobDialog },
});
</script>
