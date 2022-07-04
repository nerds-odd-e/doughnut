<template>
  <div class="btn-group">
    <button
      class="btn btn-primary loading"
      @click.once="$emit('selfEvaluatedMemoryState', 'yes')"
      :disabled="!ready"
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
import gsap from "gsap";
import { defineComponent } from "vue";

export default defineComponent({
  emits: ["selfEvaluatedMemoryState"],
  data() {
    return {
      loadingWidth: "100%",
      ready: false,
    };
  },
  mounted() {
    setTimeout(() => {
      this.ready = true;
    }, 10 * 1000);
    gsap.to(this, {
      duration: 10,
      loadingWidth: "0%",
    });
  },
});
</script>

<style lang="scss" scoped>
.loading:after {
  content: "";
  background-color: red;
  width: v-bind(loadingWidth);
  height: 100%;
  top: 0;
  right: 0;
  position: absolute;
  opacity: 0.5;
}
</style>
