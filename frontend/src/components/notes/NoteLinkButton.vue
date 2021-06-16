<template>
  <router-link :to="{name: 'noteShow', params: {noteid}}"  v-if="!!noteid" role="button" class="btn btn-sm" :title="title">
    <slot/>
  </router-link>
  <a v-else role="button" class="btn btn-sm disabled" :title="title">
    <slot/>
  </a>
</template>

<script setup>
  const props = defineProps({urlPrefix: String, noteid: Number, title: String})
</script>
