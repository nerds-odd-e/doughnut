<template>
  <ToolbarFrame>
    <template v-if="!user">
      <div class="btn-group btn-group-sm">
        <BrandBar />
      </div>
    </template>
    <div v-else class="btn-group btn-group-sm">
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
import BrandBar from "./BrandBar.vue";
import ControlCenterForNote from "./ControlCenterForNote.vue";

export default defineComponent({
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    user: { type: Object as PropType<Generated.User> },
  },
  components: {
    ToolbarFrame,
    BrandBar,
    ControlCenterForNote,
  },
  computed: {
    selectedNote(): Generated.Note | undefined {
      return this.storageAccessor.selectedNote;
    },
  },
});
</script>
