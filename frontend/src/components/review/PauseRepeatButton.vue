<template>
<router-link v-if="allowPause" class="pause-repeat" :to="!!linkId ? {name: 'linkShow', params: {linkid: linkId}} : {name: 'noteShow', params: {noteId}}"><SvgPause/></router-link>
<span v-else class="pause-repeat disabled"><SvgPause/></span>
</template>

<script>
import SvgPause from "../svgs/SvgPause.vue"

export default {
  name: "PauseRepeatButton",
  props: { noteId: Number, linkId: Number, allowPause: Boolean },
  components: { SvgPause }
};
</script>

<style lang="scss" scoped>
.pause-repeat {
  svg {
    width: 50px;
    height: 50px;
  }
  &.disabled {
      opacity: .5;
  }
}
</style>