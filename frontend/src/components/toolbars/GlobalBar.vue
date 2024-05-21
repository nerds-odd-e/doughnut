<template>
  <nav class="navbar justify-content-between global-bar sticky-top">
    <div class="container-fluid d-flex">
      <div class="btn-group">
        <PopButton v-if="user" title="open sidebar" :sidebar="'left'">
          <template #button_face>
            <SvgSidebar />
          </template>
          <GlobalSidebar
            :user="user"
            @update-user="$emit('updateUser', $event)"
          />
        </PopButton>
        <LoginButton v-else />
        <button
          role="button"
          class="d-lg-none btn btn-sm"
          title="toggle sidebar"
          @click="$emit('toggle-sidebar')"
        >
          <span class="navbar-toggler-icon"></span>
        </button>
      </div>
      <div class="d-flex flex-grow-1 justify-content-between">
        <CurrentNoteRealmLoader v-bind="{ storageAccessor }">
          <template #default="{ noteRealm }">
            <Breadcrumb
              v-if="noteRealm"
              v-bind="{ notePosition: noteRealm?.notePosition }"
            />
          </template>
        </CurrentNoteRealmLoader>
        <div class="btn-group btn-group-sm">
          <PopButton v-if="user" title="search note">
            <template #button_face>
              <SvgSearch />
            </template>
            <template #default="{ closer }">
              <LinkNoteDialog
                v-bind="{ storageAccessor }"
                @close-dialog="closer"
              />
            </template>
          </PopButton>
          <NoteUndoButton v-bind="{ storageAccessor }" />
        </div>
        <ApiStatus
          :api-status="apiStatus"
          @clear-error-message="$emit('clearErrorMessage')"
        />
      </div>
    </div>
  </nav>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import { User } from "@/generated/backend";
import { ApiStatus } from "@/managedApi/ManagedApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import PopButton from "@/components/commons/Popups/PopButton.vue";
import SvgSidebar from "@/components/svgs/SvgSidebar.vue";
import GlobalSidebar from "./GlobalSidebar.vue";

export default defineComponent({
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    apiStatus: { type: Object as PropType<ApiStatus>, required: true },
    user: { type: Object as PropType<User> },
  },
  components: {
    PopButton,
    SvgSidebar,
    GlobalSidebar,
  },
  emits: ["updateUser", "clearErrorMessage", "toggle-sidebar"],
});
</script>

<style scoped lang="scss">
.global-bar {
  background-color: #cee0fa;
}
</style>
