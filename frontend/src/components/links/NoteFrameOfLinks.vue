<template>
  <ul class="parent-links" v-if="linksReader">
    <template
      v-for="(linksOfType, _linkType) in linksReader.hierachyLinks"
      :key="_linkType"
    >
      <li v-for="link in linksOfType.direct" :key="link.id">
        <LinkOfNote v-bind="{ link, storageAccessor }" :reverse="false" />
      </li>
    </template>
    <li
      v-if="!!linksReader.groupedLinks && linksReader.groupedLinks.length > 0"
    >
      <template
        v-for="{ direct, reverse } in linksReader.groupedLinks"
        :key="direct"
      >
        <LinkOfNote
          class="link-multi"
          v-for="link in direct"
          :key="link.id"
          v-bind="{ link, storageAccessor }"
          :reverse="false"
        />
        <LinkOfNote
          class="link-multi"
          v-for="link in reverse"
          :key="link.id"
          v-bind="{ link, storageAccessor }"
          :reverse="true"
        />
      </template>
    </li>
    <template
      v-for="(linksOfType, _linkType) in linksReader.tagLinks"
      :key="_linkType"
    >
      <li v-if="linksOfType.direct.length > 0">
        <LinkOfNote
          class="link-multi"
          v-for="link in linksOfType.direct"
          :key="link.id"
          v-bind="{ link, storageAccessor }"
          :reverse="false"
        />
      </li>
    </template>
  </ul>

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
          v-bind="{ link, storageAccessor }"
          :reverse="true"
        />
      </li>
    </template>
  </ul>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkOfNote from "./LinkOfNote.vue";
import LinksReader from "../../models/LinksReader";
import { reverseLabel } from "../../models/linkTypeOptions";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    links: Object as PropType<Generated.LinksOfANote>,
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
      if (this.links && this.links.links) {
        return new LinksReader(this.links.links);
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
