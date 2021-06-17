<template>
<NoteBreadcrumb :ancestors="ancestors">
  <template v-slot:topLink>
    <template v-if="notebook.ownership.fromCircle">
      <li class="breadcrumb-item"><a href="/circles">Circles</a></li>
      <li class="breadcrumb-item">
        <a :href="`/circles/${notebook.ownership.circle.id}`">{{notebook.ownership.circle.name}}</a></li>
    </template>
    <li class="breadcrumb-item" v-else><a href="/notebooks">Top</a></li>
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
