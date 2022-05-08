<template>
  <span class="link-link">
    <LinkNob
      v-bind="{ link, colors }"
      v-if="!!reverse"
      :inverse-icon="true"
      @noteRealmUpdated="$emit('noteRealmUpdated', $event)"
    />
    <NoteTitleWithLink class="link-title" v-bind="{ note }" />
    <LinkNob
      v-bind="{ link, colors }"
      v-if="!reverse"
      :inverse-icon="false"
      @noteRealmUpdated="$emit('noteRealmUpdated', $event)"
    />
  </span>
</template>

<script></script>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue";
import LinkNob from "./LinkNob.vue";
import { colors } from "../../colors";

export default defineComponent({
  props: { link: Object, reverse: Boolean },
  emits: ["noteRealmUpdated"],
  components: { NoteTitleWithLink, LinkNob },
  computed: {
    note() {
      return this.reverse ? this.link.sourceNote : this.link.targetNote;
    },
    fontColor() {
      this.reverse ? colors.target : colors.source;
    },
    colors() {return colors; }
  },
});
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
