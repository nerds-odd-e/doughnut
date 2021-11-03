<template>
  <RadioButtons
    v-model="viewType"
    @update:modelValue="viewTypeChange"
    v-bind="{ options }"
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
import {viewTypes, viewType} from "../../models/viewTypes";
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

  computed: {
    options() {return viewTypes},
  },
  methods: {
    viewTypeChange(newType) {
      this.$router.push({name: 'noteShow', params:{ noteId: this.noteId, viewType: newType }})
    },
  },

};
</script>
