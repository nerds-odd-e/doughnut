<template>
  <teleport to="#head-status">
    <CurrentNoteRealmLoader v-bind="{ storageAccessor }">
      <template #default="{ noteRealm }">
        <Breadcrumb
          v-if="noteRealm"
          v-bind="{ notePosition: noteRealm?.notePosition }"
        />
      </template>
    </CurrentNoteRealmLoader>
  </teleport>
  <div class="d-flex flex-grow-1">
    <aside
      class="d-lg-block flex-shrink-0 overflow-auto"
      :class="{ 'd-none': sidebarCollapsedForSmallScreen }"
    >
      <NoteSidebar
        v-bind="{
          storageAccessor,
        }"
      />
    </aside>
    <main
      class="flex-grow-1 overflow-auto"
      :class="{ 'd-none': !sidebarCollapsedForSmallScreen }"
    >
      <div class="container">
        <NoteShow
          v-bind="{
            noteId,
            expandChildren: true,
            readonly: !user,
            storageAccessor,
          }"
        />
      </div>
    </main>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { User } from "@/generated/backend";
import NoteShow from "../components/notes/NoteShow.vue";
import { StorageAccessor } from "../store/createNoteStorage";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    sidebarCollapsedForSmallScreen: { type: Boolean, required: false },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    user: { type: Object as PropType<User> },
  },
  components: { NoteShow },
});
</script>

<style scoped lang="scss">
@import "bootstrap/scss/bootstrap";

aside {
  width: 100%;
  @include media-breakpoint-up(lg) {
    width: 18rem;
  }
}
</style>
