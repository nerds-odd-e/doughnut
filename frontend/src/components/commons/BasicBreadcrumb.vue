<template>
  <ol
    :style="`--bs-breadcrumb-divider: url(&#34;data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='8' height='8'%3E%3Cpath d='M2.5 0L1 1.5 3.5 4 1 6.5 2.5 8l4-4-4-4z' fill='currentColor'/%3E%3C/svg%3E&#34;);`"
    class="breadcrumb flex-nowrap bg-light bg-gradient"
  >
    <slot name="topLink" />
    <li
      class="breadcrumb-item text-truncate"
      v-for="note in ancestors"
      :key="note.id"
    >
      <NoteTitleWithLink v-bind="{ note }" />
    </li>
    <li class="breadcrumb-item" v-if="$slots.additional">
      <slot name="additional" />
    </li>
  </ol>
</template>

<script setup>
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue";

const props = defineProps({ ancestors: Array });
</script>

<style lang="scss" scoped>
.breadcrumb {
  margin-bottom: 0;
  background-color: #cee0fa;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
