<template>
  <router-link :to="{name: 'noteShow', params: {noteid}}"  v-if="!!noteid" role="button" class="btn btn-sm" :title="title">
    <slot/>
  </router-link>
</template>

<script setup>
  const props = defineProps({noteid: Number, title: String})
</script>
