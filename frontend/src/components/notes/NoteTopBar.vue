<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light">
    <div class="container-fluid d-flex">
      <button
        role="button"
        class="d-lg-none btn btn-sm"
        title="toggle sidebar"
        @click="$emit('toggle-sidebar')"
      >
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="d-flex flex-grow-1 justify-content-between">
        <CurrentNoteRealmLoader v-bind="{ storageAccessor }">
          <template #default="{ noteRealm }">
            <Breadcrumb
              v-if="noteRealm"
              v-bind="{ notePosition: noteRealm?.notePosition }"
            />
          </template>
        </CurrentNoteRealmLoader>
        <div class="btn-group">
          <span class="btn btn-sm" role="button" title="edit note">
            <SvgEdit />
          </span>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { PropType } from "vue";
import Breadcrumb from "../toolbars/Breadcrumb.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

defineProps({
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const emit = defineEmits(["toggle-sidebar"]);
</script>
