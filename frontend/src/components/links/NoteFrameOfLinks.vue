<template>
  <ul class="parent-links" v-if="linksReader">
    <template
      v-for="(linksOfType, _linkType) in linksReader.hierachyLinks"
      :key="_linkType"
    >
      <li v-for="link in linksOfType.direct" :key="link.id">
        <LinkOfNote
          v-bind="{ link, historyWriter }"
          :reverse="false"
          @note-realm-updated="$emit('noteRealmUpdated', $event)"
        />
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
          v-bind="{ link, historyWriter }"
          :reverse="false"
          @note-realm-updated="$emit('noteRealmUpdated', $event)"
        />
        <LinkOfNote
          class="link-multi"
          v-for="link in reverse"
          :key="link.id"
          v-bind="{ link, historyWriter }"
          :reverse="true"
          @note-realm-updated="$emit('noteRealmUpdated', $event)"
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
          v-bind="{ link, historyWriter }"
          :reverse="false"
          @note-realm-updated="$emit('noteRealmUpdated', $event)"
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
          v-bind="{ link, historyWriter }"
          :reverse="true"
          @note-realm-updated="$emit('noteRealmUpdated', $event)"
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
import { HistoryWriter } from "../../store/history";

export default defineComponent({
  props: {
    links: Object as PropType<Generated.LinksOfANote>,
    historyWriter: {
      type: Function as PropType<HistoryWriter>,
    },
  },
  emits: ["noteRealmUpdated"],
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
