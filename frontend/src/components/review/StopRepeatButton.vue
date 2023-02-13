<template>
  <a class="pause-stop" href="/" @click.prevent="goHome()"><SvgStop /></a>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import usePopups from "../commons/Popups/usePopups";
import SvgStop from "../svgs/SvgStop.vue";

export default defineComponent({
  name: "StopRepeatButton",
  setup() {
    return usePopups();
  },
  props: { noteId: Number, linkId: Number },
  components: { SvgStop },
  methods: {
    async goHome() {
      if (await this.popups.confirm("Confirm to leave the reviewing?")) {
        window.location.href = "/";
      }
    },
  },
});
</script>

<style lang="scss" scoped>
.pause-stop {
  margin-right: -8px;
  svg {
    width: 35px;
    height: 35px;
  }
}
</style>
