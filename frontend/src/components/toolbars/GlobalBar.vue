<template>
  <ToolbarFrame>
    <PopButton v-if="user" title="open sidebar" :sidebar="'left'">
      <template #button_face>
        <SvgSidebar />
      </template>
      <GlobalSidebar :user="user" @update-user="$emit('updateUser', $event)" />
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
        <template #default="{ closer }">
          <LinkNoteDialog v-bind="{ storageAccessor }" @close-dialog="closer" />
        </template>
      </PopButton>
      <NoteUndoButton v-bind="{ storageAccessor }" />
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
import GlobalSidebar from "./GlobalSidebar.vue";
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
    GlobalSidebar,
    ToolbarFrame,
    BrandBar,
  },
  emits: ["updateUser", "clearErrorMessage"],
});
</script>
