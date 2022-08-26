<template>
  <ToolbarFrame>
    <PopupButton title="link note">
      <template #button_face>
        <SvgSearch />
      </template>
      <template #dialog_body="{ doneHandler }">
        <LinkNoteDialog @done="doneHandler($event)" />
      </template>
    </PopupButton>
  </ToolbarFrame>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import PopupButton from "../commons/Popups/PopupButton.vue";
import SvgSearch from "../svgs/SvgSearch.vue";
import LinkNoteDialog from "../links/LinkNoteDialog.vue";
import ToolbarFrame from "./ToolbarFrame.vue";

export default defineComponent({
  emits: ["noteRealmUpdated"],
  components: {
    PopupButton,
    SvgSearch,
    LinkNoteDialog,
    ToolbarFrame,
  },
});
</script>
