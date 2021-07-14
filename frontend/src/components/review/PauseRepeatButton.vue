<template>
<router-link class="pause-repeat" :to="!!linkId ? {name: 'linkShow', params: {linkid: linkId}} : {name: 'noteShow', params: {noteId}}"><SvgPause/></router-link>
</template>

<script>
import SvgPause from "../svgs/SvgPause.vue"

export default {
  name: "PauseRepeatButton",
  props: { noteId: Number, linkId: Number },
  components: { SvgPause }
};
</script>

<style lang="scss" scoped>
.pause-repeat {
  svg {
    width: 50px;
    height: 50px;
  }
}
</style>