<template>
  <ToolbarFrame>
    <template v-if="!user">
      <div class="btn-group btn-group-sm">
        <BrandBar />
      </div>
    </template>
    <NoteControlCenterForUser v-else v-bind="{ user, storageAccessor }" />
    <div class="btn-group btn-group-sm">
      <ReviewButton class="btn" v-if="user" />
      <NoteUndoButton v-bind="{ storageAccessor }" />
      <UserActionsButton
        v-bind="{ user }"
        @update-user="$emit('updateUser', $event)"
      />
    </div>
    <LoadingThinBar v-if="apiStatus.states.length > 0" />
    <div v-if="apiStatus.lastErrorMessage">
      {{ apiStatus.lastErrorMessage }}
    </div>
  </ToolbarFrame>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ToolbarFrame from "./ToolbarFrame.vue";
import NoteUndoButton from "./NoteUndoButton.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import UserActionsButton from "./UserActionsButton.vue";
import BrandBar from "./BrandBar.vue";
import NoteControlCenterForUser from "./NoteControlCenterForUser.vue";
import ReviewButton from "./ReviewButton.vue";
import { ApiStatus } from "../../managedApi/ManagedApi";
import LoadingThinBar from "../commons/LoadingThinBar.vue";

export default defineComponent({
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    apiStatus: { type: Object as PropType<ApiStatus>, required: true },
    user: { type: Object as PropType<Generated.User> },
  },
  emits: ["updateUser"],
  components: {
    ToolbarFrame,
    NoteUndoButton,
    UserActionsButton,
    BrandBar,
    NoteControlCenterForUser,
    ReviewButton,
    LoadingThinBar,
  },
});
</script>
