<template>
  <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
    <template #default="{ noteRealm }">
      <div>
        <nav class="navbar navbar-expand-lg navbar-light bg-light">
          <button role="button" title="toggle sidebar" @click="toggleSideBar">
            <span class="navbar-toggler-icon"></span>
          </button>
        </nav>
        <div class="d-flex">
          <div
            class="d-lg-block flex-column flex-shrink-0"
            :class="{ 'd-none': sidebarCollapsedForSmallScreen }"
            id="sidebar"
            role="sidebar"
          >
            xxxx
          </div>
          <main
            class="flex-grow-1 d-lg-block"
            :class="{ 'd-none': !sidebarCollapsedForSmallScreen }"
          >
            <div class="container-fluid">
              <!-- Your main content goes here -->
              <Breadcrumb v-bind="{ notePosition: noteRealm.notePosition }" />
              <NoteShowInner
                v-bind="{
                  noteRealm,
                  expandChildren,
                  readonly,
                  storageAccessor,
                }"
              />
            </div>
          </main>
        </div>
      </div>
    </template>
  </NoteRealmLoader>
</template>

<script setup lang="ts">
import { PropType, defineProps, ref } from "vue";
import Breadcrumb from "../toolbars/Breadcrumb.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

defineProps({
  noteId: { type: Number, required: true },
  expandChildren: { type: Boolean, required: true },
  readonly: { type: Boolean, default: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const sidebarCollapsedForSmallScreen = ref(true);

const toggleSideBar = () => {
  sidebarCollapsedForSmallScreen.value = !sidebarCollapsedForSmallScreen.value;
};
</script>

<style lang="scss" scoped>
@import "bootstrap/scss/bootstrap";

#sidebar {
  width: 100%;
  @include media-breakpoint-up(lg) {
    width: 18rem;
  }
}
</style>
