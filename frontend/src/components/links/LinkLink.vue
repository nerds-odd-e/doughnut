<template>
  <span class="link-link">
    <LinkNob
      v-bind="{ link, colors }"
      v-if="!!reverse"
      :inverseIcon="true"
    />
    <NoteTitleWithLink class="link-title" v-bind="{ note }" />
    <LinkNob
      v-bind="{ link, colors }"
      v-if="!reverse"
      :inverseIcon="false"
    />
  </span>
</template>

<script>
import { computed } from "@vue/runtime-core";
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue";
import LinkNob from "./LinkNob.vue";
import { colors } from "../../colors";

export default {
  name: "LinkLink",
  props: {
    link: Object,
    reverse: Boolean,
    colors: String
  },
  components: {
    NoteTitleWithLink,
    LinkNob,
  },
  computed: {
    note() {
      return !!this.reverse ? this.link.sourceNote : this.link.targetNote;
    },
    fontColor() {
      return !!this.reverse ? this.colors["target"] : this.colors["source"];
    },
  },

}
</script>



<style scoped>
.link-link {
  padding-bottom: 3px;
  margin-right: 10px;
}

.link-title {
  padding-bottom: 3px;
  color: v-bind(fontColor);
}
</style>
