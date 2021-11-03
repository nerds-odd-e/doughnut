<template>
  <RadioButtons
    v-model="viewType"
    @update:modelValue="viewTypeChange"
    v-bind="{ options: [
      {value: 'cards', title: 'cards view'},
      {value: 'mindmap', title: 'mindmap view'},
      {value: 'article', title: 'article view'}] }"
  >
    <template #labelAddition="{ value }">
      <SvgMindmap v-if="value==='mindmap'"/>
      <SvgArticle v-if="value==='article'"/>
      <SvgCards v-if="value==='cards'"/>
    </template>
  </RadioButtons>
</template>

<script>
import RadioButtons from "../form/RadioButtons.vue";
import SvgArticle from "../svgs/SvgArticle.vue";
import SvgMindmap from "../svgs/SvgMindmap.vue";
import SvgCards from "../svgs/SvgCards.vue";
export default {
  props: {
    noteId: [String, Number],
    viewType: String,
  },
  components: {
    RadioButtons,
    SvgCards,
    SvgMindmap,
    SvgArticle,
  },

  methods: {
    viewTypeChange(newType) {
      this.$router.push({name: this.viewTypeToRouteName(newType), params:{ noteId }})
    },

    viewTypeToRouteName(newType) {
      if (newType === 'cards') return 'noteCards'
      if (newType === 'mindmap') return 'noteMindmap'
      return 'noteArticle'
    },
  },
};
</script>
