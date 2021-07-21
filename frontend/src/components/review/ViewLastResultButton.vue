<template>
    <template v-if="hasLastResult">
        <a role="button" class="last-result" title="view last result" @click="$emit('viewLastResult')"><SvgLastResult/></a>
    </template>
    <span v-else class="last-result disabled"><SvgLastResult/></span>
</template>

<script>
import SvgLastResult from "../svgs/SvgLastResult.vue"

export default {
  props: { hasLastResult: Boolean },
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