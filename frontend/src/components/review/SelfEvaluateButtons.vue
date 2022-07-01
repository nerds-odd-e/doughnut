<template>
  <div class="btn-group" role="group" aria-label="First group">
    <button
      class="btn btn-light"
      id="repeat-sad"
      name="sad"
      @click.once="processForm(-5)"
      title="reduce next repeat interval (days) by half"
    >
      <SvgSad />
    </button>
    <button
      class="btn btn-light"
      id="repeat-happy"
      name="happy"
      @click="processForm(5)"
      title="add to next repeat interval (days) by half"
    >
      <SvgHappy />
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import SvgSad from "../svgs/SvgSad.vue";
import SvgHappy from "../svgs/SvgHappy.vue";

export default defineComponent({
  emits: ["selfEvaluate"],
  components: { SvgSad, SvgHappy },
  methods: {
    processForm(evaluation: number) {
      this.$emit("selfEvaluate", evaluation);
    },
  },
});
</script>
