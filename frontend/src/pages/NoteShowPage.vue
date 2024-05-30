<template>
  <teleport to="#head-status">
    <div class="btn-group">
      <button
        role="button"
        class="d-md-none btn btn-sm"
        title="toggle sidebar"
        @click="toggleSideBar"
      >
        <span class="navbar-toggler-icon"></span>
      </button>
    </div>
    <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
      <template #default="{ noteRealm }">
        <ScrollTo />
        <Breadcrumb
          v-if="noteRealm"
          v-bind="{
            fromBazaar: noteRealm?.fromBazaar,
            circle: noteRealm?.circle,
            noteTopic: noteRealm?.note.noteTopic,
          }"
        />
      </template>
    </NoteRealmLoader>
  </teleport>
  <div class="d-flex flex-grow-1 overflow-auto h-full">
    <aside
      class="d-md-block flex-shrink-0 overflow-auto me-3"
      :class="{ 'd-none': sidebarCollapsedForSmallScreen }"
    >
      <NoteSidebar
        v-bind="{
          noteId,
          storageAccessor,
        }"
      />
    </aside>
    <main
      class="flex-grow-1 overflow-auto"
      :class="{ 'd-none': !sidebarCollapsedForSmallScreen }"
    >
      <NoteShow
        v-bind="{
          noteId,
          expandChildren: true,
          readonly: !user,
          storageAccessor,
        }"
      />
    </main>
  </div>
</template>

<script setup lang="ts">
import { PropType, ref } from "vue";
import { User } from "@/generated/backend";
import ScrollTo from "@/components/commons/ScrollTo.vue";
import NoteShow from "../components/notes/NoteShow.vue";
import Breadcrumb from "../components/toolbars/Breadcrumb.vue";
import { StorageAccessor } from "../store/createNoteStorage";

defineProps({
  noteId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
  user: { type: Object as PropType<User> },
});

const sidebarCollapsedForSmallScreen = ref(true);

const toggleSideBar = () => {
  sidebarCollapsedForSmallScreen.value = !sidebarCollapsedForSmallScreen.value;
};
</script>

<style scoped lang="scss">
@import "bootstrap/scss/bootstrap";

aside {
  width: 100%;
  @include media-breakpoint-up(md) {
    width: 18rem;
  }
}

.h-full {
  height: calc(100vh - 4rem);
  max-width: calc(-webkit-fill-available - 4rem);
}
</style>
