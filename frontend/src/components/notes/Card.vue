<template>
  <div class="card">
    <slot name="cardHeader" />
    <div class="card-body">
      <h5 class="card-title">
        <component :is="linkFragment" :note="note" class="card-title" />
      </h5>
      <p>{{ note.shortDescription }}</p>
      <slot name="button" :note="note" />
    </div>
  </div>
</template>

<script setup>
import NoteTitleWithLink from "./NoteTitleWithLink.vue";
const props = defineProps({
  note: Object,
  linkFragment: { type: Object, default: NoteTitleWithLink },
});
</script>
