<template>
    <template v-if="!!lastResult">
        <button class="last-result" title="view last result" @click="$emit('viewLastResult')"><SvgLastResult/></button>
    </template>
    <span v-else class="last-result disabled"><SvgLastResult/></span>
</template>

<script>
import SvgLastResult from "../svgs/SvgLastResult.vue"

export default {
  props: { lastResult: Object },
  emits: ['viewLastResult'],
  components: { SvgLastResult }
};
</script>

<style lang="scss" scoped>
.last-result {
  svg {
    width: 50px;
    height: 50px;
  }
  &.disabled {
      opacity: .5;
  }
}
</style>