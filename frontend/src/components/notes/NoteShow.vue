<template>
  <NoteTopBar
    v-bind="{
      noteId: currentNoteId?.id,
      storageAccessor,
    }"
    @toggle-sidebar="toggleSideBar"
  />
  <div class="d-flex">
    <div
      class="d-lg-block flex-column flex-shrink-0 sidebar"
      :class="{ 'd-none': sidebarCollapsedForSmallScreen }"
      role="sidebar"
    >
      <Sidebar
        v-bind="{
          noteRealm,
          storageAccessor,
        }"
      />
    </div>
    <main
      class="flex-grow-1 d-lg-block"
      :class="{ 'd-none': !sidebarCollapsedForSmallScreen }"
    >
      <div class="container-fluid">
        <ContentLoader v-if="!noteRealm" />
        <div v-else>
          <NoteShowInner
            v-bind="{
              noteRealm,
              expandChildren,
              readonly,
              storageAccessor,
            }"
            :key="noteId"
          />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { PropType, computed, ref, toRefs, watch } from "vue";
import ContentLoader from "@/components/commons/ContentLoader.vue";
import Sidebar from "./Sidebar.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

const props = defineProps({
  noteId: { type: Number, required: true },
  expandChildren: { type: Boolean, required: true },
  readonly: { type: Boolean, default: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const reactiveProps = toRefs(props);

const noteRealmRef = computed(() =>
  reactiveProps.storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndReloadPosition(reactiveProps.noteId.value),
);

const noteRealm = computed(() => noteRealmRef.value?.value);

const currentNoteIdRef = computed(() =>
  reactiveProps.storageAccessor.value.currentNoteIdRef(),
);

const currentNoteId = computed(() => currentNoteIdRef.value?.value);

watch(
  () => reactiveProps.noteId.value,
  (newNoteId) => {
    reactiveProps.storageAccessor.value.currentNoteIdRef().value = {
      id: newNoteId,
    };
  },
  { immediate: true },
);

const sidebarCollapsedForSmallScreen = ref(true);

const toggleSideBar = () => {
  sidebarCollapsedForSmallScreen.value = !sidebarCollapsedForSmallScreen.value;
};
</script>

<style lang="scss" scoped>
@import "bootstrap/scss/bootstrap";

.sidebar {
  width: 100%;
  @include media-breakpoint-up(lg) {
    width: 18rem;
  }
}
</style>
