<template>
  <div class="btn-group" role="group" aria-label="First group">
    <button
      class="btn btn-light"
      id="repeat-sad"
      name="sad"
      @click.once="processForm('sad')"
      title="reduce next repeat interval (days) by half"
    >
      <SvgSad />
    </button>
    <button
      class="btn btn-light"
      id="repeat-happy"
      name="happy"
      @click="processForm('happy')"
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
    processForm(evaluation: Generated.SelfEvaluate) {
      this.$emit("selfEvaluate", evaluation);
    },
  },
});
</script>
