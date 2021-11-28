<template>
  <div class="row">
    <div
      :class="`col-12 col-sm-6 col-md-${12 / (columns - 1)} col-lg-${
        12 / columns
      }`"
      v-for="note in notes"
      :key="note.id"
    >
      <Card :note="note">
        <template #cardHeader>
          <slot name="cardHeader" :note="note" />
        </template>
        <template #button>
          <slot name="button" :note="note" />
        </template>
      </Card>
    </div>
  </div>
</template>

<script>
import Card from "./Card.vue";
import NoteTitleWithLink from "./NoteTitleWithLink.vue";

export default {
  props: {
    notes: Array,
    columns: { type: Number, default: 4 },
  },
  components: { Card, NoteTitleWithLink },
};
</script>
