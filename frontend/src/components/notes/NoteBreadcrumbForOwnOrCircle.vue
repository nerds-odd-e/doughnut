<template>
<NoteBreadcrumb :ancestors="ancestors">
  <template v-slot:topLink>
    <template v-if="!!notebook && notebook.ownership.fromCircle">
      <li class="breadcrumb-item"><a href="/circles">Circles</a></li>
      <li class="breadcrumb-item">
        <a :href="`/circles/${notebook.ownership.circle.id}`">{{notebook.ownership.circle.name}}</a></li>
    </template>
    <li class="breadcrumb-item" v-else><router-link :to="{name: 'notebooks'}">Top</router-link></li>
  </template>
  <template v-slot:additional>
    <slot />
  </template>
</NoteBreadcrumb>
</template>

<script setup>
  import NoteBreadcrumb from "./NoteBreadcrumb.vue"
  const props = defineProps({ancestors: Array, notebook: Object})
</script>
