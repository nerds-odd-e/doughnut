<template>
  <PopButton class="btn-primary" title="Suggest">
    <NoteSuggestDescriptionDialog v-bind="{ selectedNote, storageAccessor }" />
  </PopButton>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteSuggestDescriptionDialog from "./NoteSuggestDescriptionDialog.vue";
import PopButton from "../commons/Popups/PopButton.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    selectedNote: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { PopButton, NoteSuggestDescriptionDialog },
});
</script>

<style lang="sass" scoped>
.btn-primary
  align-items: center
</style>
