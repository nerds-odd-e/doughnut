<template>
  <template v-if="!!note">
    <router-link
      :to="{ name: 'noteShow', params: { noteId: note.id } }"
      class="text-decoration-none"
    >
      {{ note.title }}
    </router-link>
  </template>
</template>

<script setup>
const props = defineProps({ note: Object });
</script>
