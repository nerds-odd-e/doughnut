<template>
  <div>
    <SvgMindmap v-if="viewType === 'mindmap'" />
    <SvgArticle v-if="viewType === 'article'" />
    <SvgCards v-if="viewType === 'cards'" />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import SvgArticle from "../svgs/SvgArticle.vue";
import SvgMindmap from "../svgs/SvgMindmap.vue";
import SvgCards from "../svgs/SvgCards.vue";

export default defineComponent({
  props: { viewType: String },
  components: {
    SvgCards,
    SvgMindmap,
    SvgArticle,
  },
});
</script>
