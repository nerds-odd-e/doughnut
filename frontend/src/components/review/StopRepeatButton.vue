<template>
<a class="pause-stop" href="/" @click.prevent="goHome()"><SvgStop/></a>
</template>

<script>
import SvgStop from "../svgs/SvgStop.vue"

export default {
  name: "StopRepeatButton",
  props: { noteId: Number, linkId: Number },
  components: { SvgStop },
  methods: {
    async goHome() {
      if(await this.$popups.confirm('Are you sure to leave the reviewing?')) {
        window.location = "/"
      }
    }
  }
};
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