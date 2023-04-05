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
    <div class="btn-group btn-group-sm">
      <ReviewButton class="btn" v-if="user" />
      <NoteUndoButton v-bind="{ storageAccessor }" />
      <UserActionsButton
        v-bind="{ user }"
        @update-user="$emit('updateUser', $event)"
      />
    </div>
    <ApiStatus
      :api-status="apiStatus"
      @clear-error-message="$emit('clearErrorMessage')"
    />
  </ToolbarFrame>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ToolbarFrame from "./ToolbarFrame.vue";
import NoteUndoButton from "./NoteUndoButton.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import UserActionsButton from "./UserActionsButton.vue";
import BrandBar from "./BrandBar.vue";
import ControlCenterForNote from "./ControlCenterForNote.vue";
import ReviewButton from "./ReviewButton.vue";
import { ApiStatus } from "../../managedApi/ManagedApi";

export default defineComponent({
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    apiStatus: { type: Object as PropType<ApiStatus>, required: true },
    user: { type: Object as PropType<Generated.User> },
  },
  emits: ["updateUser", "clearErrorMessage"],
  components: {
    ToolbarFrame,
    NoteUndoButton,
    UserActionsButton,
    BrandBar,
    ControlCenterForNote,
    ReviewButton,
  },
  computed: {
    selectedNote(): Generated.Note | undefined {
      return this.storageAccessor.selectedNote;
    },
  },
});
</script>
