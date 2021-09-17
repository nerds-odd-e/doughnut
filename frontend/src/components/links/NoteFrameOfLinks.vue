<template>
  <ul class="parent-links">
    <template v-for="(linksOfType, linkType) in linksReader.hierachyLinks" :key="linkType">
      <li v-for="link in linksOfType.direct" :key="link.id">
        <LinkLink
          v-bind="{ link, colors: $staticInfo.colors }"
          :reverse="false"
          @updated="$emit('updated')"
        />
      </li>
    </template>
    <li v-if="!!linksReader.groupedLinks && linksReader.groupedLinks.length > 0">
      <template v-for="{ direct, reverse } in linksReader.groupedLinks" :key="direct">
        <LinkLink
          class="link-multi"
          v-for="link in direct"
          :key="link.id"
          v-bind="{ link, colors: $staticInfo.colors }"
          :reverse="false"
          @updated="$emit('updated')"
        />
        <LinkLink
          class="link-multi"
          v-for="link in reverse"
          :key="link.id"
          v-bind="{ link, colors: $staticInfo.colors }"
          :reverse="true"
          @updated="$emit('updated')"
        />
      </template>
    </li>
    <template v-for="(linksOfType, linkType) in linksReader.tagLinks" :key="linkType">
      <li v-if="linksOfType.direct.length > 0">
        <LinkLink
          class="link-multi"
          v-for="link in linksOfType.direct"
          :key="link.id"
          v-bind="{ link, colors: $staticInfo.colors }"
          :reverse="false"
          @updated="$emit('updated')"
        />
      </li>
    </template>
  </ul>
  
  <slot />

  <ul class="children-links">
    <template v-for="(linksOfType, linkType) in linksReader.childrenLinks" :key="linkType">
      <li v-if="linksOfType.reverse.length > 0">
        <span>{{ linksReader.reverseLabel(linkType) }} </span>
        <LinkLink
          class="link-multi"
          v-for="link in linksOfType.reverse"
          :key="link.id"
          v-bind="{ link, colors: $staticInfo.colors }"
          :reverse="true"
          @updated="$emit('updated')"
        />
      </li>
    </template>
  </ul>
</template>

<script>
import LinkLink from "./LinkLink.vue";
import LinksReader from "../../models/LinksReader"

export default {
  props: { links: Object },
  emits: ["updated"],
  components: { LinkLink },
  computed: {
    linksReader() {
      if (!this.$staticInfo || !this.$staticInfo.linkTypeOptions) {
        return {}
      }
      if(!this.links) return {}
      return new LinksReader(this.$staticInfo.linkTypeOptions, this.links)
    }
  }
}

</script>

<style scoped>
.parent-links,
.children-links {
  list-style: none;
  padding: 0px;
  margin: 0px;
  background-color: #eee;
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
