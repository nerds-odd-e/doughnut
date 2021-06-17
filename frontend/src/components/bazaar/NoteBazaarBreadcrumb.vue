<template>
<NoteBreadcrumb :ancestors="ancestors">
    <template #additional>
      <slot />
    </template>
    <template #topLink>
      <li class="breadcrumb-item"><a href="/bazaar">Bazaar</a></li>
    </template>
</NoteBreadcrumb>
</template>

<script setup>
  import NoteBreadcrumb from "../notes/NoteBreadcrumb.vue"
  const props = defineProps({ancestors: Array})
</script>
