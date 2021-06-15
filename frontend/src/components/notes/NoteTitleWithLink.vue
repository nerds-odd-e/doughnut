<template>
  <template v-if="!!note">
    <a :href="`/notes/${note.id}`">{{note.title}}</a>
  </template>
</template>

<script setup>
  const props = defineProps({note: Object})
</script>
