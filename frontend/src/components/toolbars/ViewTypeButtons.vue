<template>
  <RadioButtons
    :model-value="viewType"
    @update:model-value="viewTypeChange"
    v-bind="{ options }"
  >
    <template #labelAddition="{ value }">
      <SvgMindmap v-if="value === 'mindmap'" />
      <SvgArticle v-if="value === 'article'" />
      <SvgCards v-if="value === 'cards'" />
    </template>
  </RadioButtons>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import RadioButtons from "../form/RadioButtons.vue";
import SvgArticle from "../svgs/SvgArticle.vue";
import SvgMindmap from "../svgs/SvgMindmap.vue";
import SvgCards from "../svgs/SvgCards.vue";
import { routeNameForViewType, viewTypeNames } from "../../models/viewTypes";

export default defineComponent({
  props: {
    noteId: Number,
    viewType: String,
  },
  components: {
    RadioButtons,
    SvgCards,
    SvgMindmap,
    SvgArticle,
  },

  computed: {
    options() {
      return viewTypeNames.map((name) => ({
        value: name,
        title: `${name} view`,
      }));
    },
  },
  methods: {
    viewTypeChange(newType) {
      this.$router.push({
        name: routeNameForViewType(newType),
        params: { noteId: this.noteId },
      });
    },
  },
});
</script>
