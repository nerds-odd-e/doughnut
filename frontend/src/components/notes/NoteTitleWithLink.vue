<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: note.id } }"
    class="text-decoration-none"
  >
    {{ note.topic }}
  </router-link>
</template>

<script>
export default {
  props: {
    note: { type: Object, required: true },
  },
};
</script>
