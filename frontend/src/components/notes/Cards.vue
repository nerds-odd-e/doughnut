<template>
  <div class="row">
    <div
      :class="`col-12 col-sm-6 col-md-${12 / (columns - 1)} col-lg-${
        12 / columns
      }`"
      v-for="note in notes"
      :key="note.id"
    >
      <Card v-bind="{ note, parentNote }">
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

<script lang="ts">
import { PropType, defineComponent } from "vue";
import Card from "./Card.vue";

export default defineComponent({
  props: {
    notes: { type: Array as PropType<Generated.Note[]>, required: true },
    columns: { type: Number, default: 4 },
    parentNote: { type: Object as PropType<Generated.Note> },
  },
  components: { Card },
});
</script>
