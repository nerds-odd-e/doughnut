<template>
  <LinkType
    v-for="(_, linkType, index) in directLinks"
    :key="linkType"
    v-bind="{
      linkType,
      totalLinkTypeCount: directLinkTypeCount,
      index,
      mindmapSector,
      mindmap,
    }"
  />
  <LinkType
    v-for="(_, linkType, index) in reverseLinks"
    :key="linkType"
    v-bind="{
      reverse: true,
      linkType,
      totalLinkTypeCount: reverseLinkTypeCount,
      index,
      mindmapSector,
      mindmap,
    }"
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import Mindmap from "../../../models/Mindmap";
import MindmapSector from "../../../models/MindmapSector";
import LinksReader from "../../../models/LinksReader";
import LinkType from "./LinkType.vue";
import LinksMap from "../../../models/LinksMap";

export default defineComponent({
  props: {
    links: {
      type: Object as PropType<LinksMap>,
      required: true,
    },
    mindmapSector: { type: Object as PropType<MindmapSector>, required: true },
    mindmap: { type: Object as PropType<Mindmap>, required: true },
  },
  components: { LinkType },
  computed: {
    directLinks() {
      return new LinksReader(this.links).directLinks;
    },
    reverseLinks() {
      return new LinksReader(this.links).reverseLinks;
    },
    directLinkTypeCount() {
      return Object.keys(this.directLinks).length;
    },
    reverseLinkTypeCount() {
      return Object.keys(this.reverseLinks).length;
    },
  },
});
</script>

<style lang="sass" scoped></style>
