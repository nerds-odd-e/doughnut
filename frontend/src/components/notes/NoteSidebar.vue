<template>
  <CurrentNoteRealmLoader v-bind="{ storageAccessor }">
    <template #default="{ noteRealm }">
      <Sidebar
        v-bind="{
          noteRealm,
          storageAccessor,
        }"
      />
    </template>
  </CurrentNoteRealmLoader>
</template>

<script setup lang="ts">
import { PropType } from "vue";
import { StorageAccessor } from "../../store/createNoteStorage";

defineProps({
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});
</script>
