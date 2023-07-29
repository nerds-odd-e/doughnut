<template>
  <div class="btn-group">
    <button
      class="btn btn-primary loading"
      @click.once="$emit('self-evaluated-memory-state', true)"
      :disabled="!ready"
    >
      Yes, I remember
    </button>
    <button
      class="btn btn-secondary"
      @click.once="$emit('self-evaluated-memory-state', false)"
    >
      No, I need more repetition
    </button>
  </div>
</template>

<script lang="ts">
import gsap from "gsap";
import { defineComponent } from "vue";

export default defineComponent({
  emits: ["self-evaluated-memory-state"],
  data() {
    return {
      loadingWidth: "100%",
      ready: false,
    };
  },
  methods: {
    readyIn(sec: number) {
      //
      // gsap is not testable with Cypress,
      // so we need to use another timeout,
      // which is testable with Cypress.
      //
      // In Cypress, `cy.clock()` with no arguments and then tick for more than 1 second
      // will cause any component depending on v-if="aysncly loaded ref" to fail.
      //
      // cy.clock(null, ['setTimeout']) works.
      //
      // This is probably a bug of Cypress, or a bug in between Cypress and VueJs mounted().
      //
      //
      // Also, it seems that gsap depnds on window.Date.
      // But Cypress doesn't have a clear way of mocking window.Date.
      // `cy.clock()` with no arguments works, but it break other things as mentioned above.
      setTimeout(() => {
        this.ready = true;
      }, sec * 1000);
      gsap.to(this, {
        duration: sec,
        loadingWidth: "0%",
      });
    },
  },
  mounted() {
    this.readyIn(10);
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
