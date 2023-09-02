<template>
  <ToolbarFrame>
    <PopButton v-if="user" title="open sidebar" :sidebar="'left'">
      <template #button_face>
        <SvgSidebar />
      </template>
      <CircleSelector />
    </PopButton>
    <LoginButton v-else />
    <div class="btn-group btn-group-sm">
      <BrandBar />
    </div>
    <div class="btn-group btn-group-sm">
      <PopButton v-if="user" title="search note">
        <template #button_face>
          <SvgSearch />
        </template>
        <LinkNoteDialog v-bind="{ storageAccessor }" />
      </PopButton>
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
import { PropType, defineComponent } from "vue";
import PopButton from "../commons/Popups/PopButton.vue";
import SvgSidebar from "../svgs/SvgSidebar.vue";
import CircleSelector from "../circles/CircleSelector.vue";
import ToolbarFrame from "./ToolbarFrame.vue";
import { ApiStatus } from "../../managedApi/ManagedApi";
import { StorageAccessor } from "../../store/createNoteStorage";
import BrandBar from "./BrandBar.vue";

export default defineComponent({
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    apiStatus: { type: Object as PropType<ApiStatus>, required: true },
    user: { type: Object as PropType<Generated.User> },
  },
  components: {
    PopButton,
    SvgSidebar,
    CircleSelector,
    ToolbarFrame,
    BrandBar,
  },
  emits: ["updateUser", "clearErrorMessage"],
});
</script>
