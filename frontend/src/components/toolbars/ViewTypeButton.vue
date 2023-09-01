<template>
  <div>
    <SvgCards v-if="viewType === 'cards'" />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import SvgCards from "../svgs/SvgCards.vue";

export default defineComponent({
  props: { viewType: String },
  components: {
    SvgCards,
  },
});
</script>
