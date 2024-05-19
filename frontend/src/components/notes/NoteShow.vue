<template>
  <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
    <template #default="{ noteRealm }">
      <div>
        <nav class="navbar navbar-expand-lg navbar-light bg-light">
          <button
            class="navbar-toggler"
            type="button"
            data-bs-toggle="collapse"
            data-bs-target="#sidebar"
            aria-controls="sidebar"
            aria-expanded="false"
            aria-label="Toggle navigation"
          >
            <span class="navbar-toggler-icon"></span>
          </button>
          <a class="navbar-brand" href="#">Brand</a>
        </nav>
        <div class="d-flex">
          <div
            class="d-lg-block collapse flex-column flex-shrink-0"
            id="sidebar"
          >
            xxxx
          </div>
          <main class="flex-grow-1">
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
import { PropType, defineProps } from "vue";
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
