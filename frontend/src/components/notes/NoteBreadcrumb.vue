<template>
  <div>
    <ol
      style="
        --bs-breadcrumb-divider: url(
          &#34;data:image/svg + xml,
          %3Csvg xmlns='http://www.w3.org/2000/svg'width='8'height='8'%3E%3Cpath d='M2.5 0L1 1.5 3.5 4 1 6.5 2.5 8l4-4-4-4z'fill='currentColor'/%3E%3C/svg%3E&#34;
        );
      "
      class="breadcrumb bg-light bg-gradient"
    >
      <slot name="topLink" />
      <li class="breadcrumb-item" v-for="note in ancestors" :key="note.id">
        <component v-if="!!linkFragment" :is="linkFragment" :note="note" />
        <NoteTitleWithLink v-else :note="note" />
      </li>
      <slot name="additional" />
    </ol>
  </div>
</template>

<script setup>
import NoteTitleWithLink from "./NoteTitleWithLink.vue";
const props = defineProps({ ancestors: Array, linkFragment: Object });
const name = "";
</script>

<style scoped></style>
