<template>
  <template v-if="allowPause">
    <router-link
      v-if="btn === 'pause'"
      class="pause-repeat"
      :to="
        !!linkId
          ? { name: 'linkShow', params: { linkid: linkId } }
          : { name: 'noteShow', params: { noteId } }
      "
      ><SvgPause
    /></router-link>
    <a href="#" @click.prevent v-else class="pause-repeat"><SvgResume /></a>
  </template>
  <span v-else class="pause-repeat disabled"><SvgPause /></span>
</template>

<script>
import SvgPause from "../svgs/SvgPause.vue";
import SvgResume from "../svgs/SvgResume.vue";

export default {
  name: "PauseRepeatButton",
  props: { noteId: Number, linkId: Number, allowPause: Boolean, btn: String },
  components: { SvgPause, SvgResume },
};
</script>

<style lang="scss" scoped>
.pause-repeat {
  svg {
    width: 50px;
    height: 50px;
  }
  &.disabled {
    opacity: 0.5;
  }
}
</style>
