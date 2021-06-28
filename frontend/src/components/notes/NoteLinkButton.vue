<template>
  <RelativeRouterLink :to="{name: 'noteShow', params: {noteid}}"  v-if="!!noteid" role="button" class="btn btn-sm" :title="title">
    <slot/>
  </RelativeRouterLink>
  <a v-else role="button" class="btn btn-sm disabled" :title="title">
    <slot/>
  </a>
</template>

<script setup>
  import RelativeRouterLink from "../../routes/RelativeRouterLink.vue"
  const props = defineProps({noteid: Number, title: String})
</script>
