<template>
  <ToolbarFrame>
    <div class="btn-group btn-group-sm">
      <template v-if="!selectedNote">
        <PopButton title="search note">
          <template #button_face>
            <SvgSearch />
          </template>
          <LinkNoteDialog v-bind="{ storageAccessor }" />
        </PopButton>
      </template>
      <ControlCenterForNote
        v-if="selectedNote"
        v-bind="{ selectedNote, storageAccessor }"
      />
    </div>
  </ToolbarFrame>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ToolbarFrame from "./ToolbarFrame.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import ControlCenterForNote from "./ControlCenterForNote.vue";

export default defineComponent({
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    ToolbarFrame,
    ControlCenterForNote,
  },
  computed: {
    selectedNote(): Generated.Note | undefined {
      return this.storageAccessor.selectedNote;
    },
  },
});
</script>
