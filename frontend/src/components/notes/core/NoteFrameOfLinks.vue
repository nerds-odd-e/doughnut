<template>
  <slot />

  <ul class="children-links" v-if="linksReader">
    <template
      v-for="(linksOfType, linkType) in linksReader.childrenLinks"
      :key="linkType"
    >
      <li v-if="linksOfType.reverse.length > 0">
        <span>{{ reverseLabel(linkType) }} </span>
        <LinkOfNote
          class="link-multi"
          v-for="link in linksOfType.reverse"
          :key="link.id"
          v-bind="{ note: link, storageAccessor }"
          :reverse="true"
        />
      </li>
    </template>
  </ul>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkOfNote from "../../links/LinkOfNote.vue";
import LinksReader from "../../../models/LinksReader";
import { reverseLabel } from "../../../models/linkTypeOptions";
import { StorageAccessor } from "../../../store/createNoteStorage";
import LinksMap from "../../../models/LinksMap";

export default defineComponent({
  props: {
    links: Object as PropType<LinksMap>,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { LinkOfNote },
  methods: {
    reverseLabel(lbl) {
      return reverseLabel(lbl);
    },
  },
  computed: {
    linksReader() {
      if (this.links) {
        return new LinksReader(this.links);
      }
      return undefined;
    },
  },
});
</script>

<style scoped>
.parent-links,
.children-links {
  list-style: none;
  padding: 0px;
  margin: 0px;
}

.parent-links {
  border-top-right-radius: 10px;
  border-top-left-radius: 10px;
}

.children-links {
  border-bottom-right-radius: 10px;
  border-bottom-left-radius: 10px;
}

.parent-links li {
  border-top-right-radius: 10px;
  border-top-left-radius: 10px;
  border-top: solid 1px black;
  padding-right: 10px;
  padding-left: 10px;
}

.children-links li {
  border-right: 10px;
  border-bottom-right-radius: 10px;
  border-bottom-left-radius: 10px;
  border-bottom: solid 1px black;
  padding-right: 10px;
  padding-left: 10px;
}

.link-multi + .link-multi::before {
  padding-right: 0.5rem;
  color: #6c757d;
  content: "|";
}
</style>
