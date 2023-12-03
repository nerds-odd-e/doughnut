<template>
  <span class="link-nob">
    <PopButton1 :title="link.linkType">
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
    </PopButton1>
  </span>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";
import PopButton1 from "../commons/Popups/PopButton1.vue";
import LinkNobDialog from "./LinkNobDialog.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    link: {
      type: Object as PropType<Generated.Link>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    inverseIcon: Boolean,
    colors: Object,
  },
  components: { SvgLinkTypeIcon, PopButton1, LinkNobDialog },
});
</script>
