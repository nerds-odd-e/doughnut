<template>
  <span class="link-nob">
    <PopButton v-if="link.linkType" :title="link.linkType">
      <template #button_face>
        <SvgLinkTypeIcon
          :link-type="link.linkType"
          :inverse-icon="inverseIcon"
        />
      </template>
      <template #default="{ closer }">
        <LinkNobDialog
          v-bind="{ link, inverseIcon, colors, storageAccessor }"
          @close-dialog="closer"
        />
      </template>
    </PopButton>
  </span>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Thing } from "@/generated/backend";
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";
import PopButton from "../commons/Popups/PopButton.vue";
import LinkNobDialog from "./LinkNobDialog.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    link: {
      type: Object as PropType<Thing>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    inverseIcon: Boolean,
    colors: Object,
  },
  components: { SvgLinkTypeIcon, PopButton, LinkNobDialog },
});
</script>
