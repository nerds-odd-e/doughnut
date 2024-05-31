<template>
  <div
    :class="`row row-cols-1 row-cols-md-${columns - 1} row-cols-lg-${columns} g-3`"
  >
    <div class="col" v-for="note in notes" :key="note.id">
      <Card v-bind="{ noteTopic: note.noteTopic }">
        <template #button v-if="$slots.button">
          <slot name="button" :note-topic="note.noteTopic" />
        </template>
      </Card>
    </div>
  </div>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import { Note } from "@/generated/backend";
import Card from "./Card.vue";

export default defineComponent({
  props: {
    notes: { type: Array as PropType<Note[]>, required: true },
    columns: { type: Number, default: 4 },
  },
  components: { Card },
});
</script>
