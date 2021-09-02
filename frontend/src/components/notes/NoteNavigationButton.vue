<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId } }"
    v-if="!!noteId"
    role="button"
    class="btn btn-sm"
    :title="title"
  >
    <slot />
  </router-link>
  <a v-else role="button" class="btn btn-sm disabled" :title="title">
    <slot />
  </a>
</template>

<script setup>
const props = defineProps({ noteId: Number, title: String });
</script>
