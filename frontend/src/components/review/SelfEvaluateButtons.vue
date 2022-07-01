<template>
  <div class="btn-group">
    <button
      class="btn btn-primary"
      @click.once="$emit('selfEvaluatedMemoryState', 'yes')"
    >
      Yes, I remember
    </button>
    <button
      class="btn btn-secondary"
      @click.once="$emit('selfEvaluatedMemoryState', 'no')"
    >
      No, I need more repetition
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  emits: ["selfEvaluatedMemoryState"],
});
</script>
