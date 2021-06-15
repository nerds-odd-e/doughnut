<template>
<div>
    <a role="button" class="btn btn-sm" :title="title" v-if="!!noteid" :href="`${urlPrefix}/notes/${note.id}`">
      <slot/>
    </a>
    <a v-else role="button" class="btn btn-sm disabled">
      <slot/>
    </a>
</div>
</template>

<script setup>
  const props = defineProps({urlPrefix: String, noteid: Number, title: String, contentTag: Object})
</script>
