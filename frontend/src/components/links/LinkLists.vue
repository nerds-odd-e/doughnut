<template>
  <ul class="parent-links">
    <template v-for="(linksOfType, linkType) in hierachyLinks" :key="linkType">
      <li v-for="link in linksOfType.direct" :key="link.id">
        <LinkLink
          v-bind="{ link, owns, colors: staticInfo.colors }"
          :reverse="false"
          @updated="$emit('updated')"
        />
      </li>
    </template>
    <li v-if="!!groupedLinks && groupedLinks.length > 0">
      <template v-for="{ direct, reverse } in groupedLinks" :key="direct">
        <LinkLink
          class="link-multi"
          v-for="link in direct"
          :key="link.id"
          v-bind="{ link, owns, colors: staticInfo.colors }"
          :reverse="false"
          @updated="$emit('updated')"
        />
        <LinkLink
          class="link-multi"
          v-for="link in reverse"
          :key="link.id"
          v-bind="{ link, owns, colors: staticInfo.colors }"
          :reverse="true"
          @updated="$emit('updated')"
        />
      </template>
    </li>
    <template v-for="(linksOfType, linkType) in tagLinks" :key="linkType">
      <li v-if="linksOfType.direct.length > 0">
        <LinkLink
          class="link-multi"
          v-for="link in linksOfType.direct"
          :key="link.id"
          v-bind="{ link, owns, colors: staticInfo.colors }"
          :reverse="false"
          @updated="$emit('updated')"
        />
      </li>
    </template>
  </ul>
  <slot />

  <ul class="children-links">
    <template v-for="(linksOfType, linkType) in hierachyLinks" :key="linkType">
      <li v-if="linksOfType.reverse.length > 0">
        <span>{{ reverseLabel(linkType) }} </span>
        <LinkLink
          class="link-multi"
          v-for="link in linksOfType.reverse"
          :key="link.id"
          v-bind="{ link, owns, colors: staticInfo.colors }"
          :reverse="true"
          @updated="$emit('updated')"
        />
      </li>
    </template>
  </ul>
</template>

<script setup>
import { computed } from "@vue/runtime-core";
import LinkLink from "./LinkLink.vue";

const props = defineProps({ links: Object, owns: Boolean, staticInfo: Object });
const emits = defineEmits(["updated"]);
const reverseLabel = function (lbl) {
  if (!props.staticInfo || !props.staticInfo.linkTypeOptions) {
    return;
  }
  const { reversedLabel } = props.staticInfo.linkTypeOptions.find(
    ({ label }) => lbl === label
  );
  return reversedLabel;
};

const taggingTypes = () => {
  if (!props.staticInfo || !props.staticInfo.linkTypeOptions) return [];
  return props.staticInfo.linkTypeOptions
    .filter((t) => t.value == 8)
    .map((t) => t.label);
};

const groupedTypes = () => {
  if (!props.staticInfo || !props.staticInfo.linkTypeOptions) return [];
  return props.staticInfo.linkTypeOptions
    .filter((t) => [1, 12, 22, 23].includes(parseInt(t.value)))
    .map((t) => t.label);
};

const hierachyLinks = computed(() => {
  const tTypes = taggingTypes();
  const gTypes = groupedTypes();
  if (!props.links) return;
  return Object.fromEntries(
    Object.entries(props.links).filter(
      (t) => !tTypes.includes(t[0]) && !gTypes.includes(t[0])
    )
  );
});

const tagLinks = computed(() => {
  const tTypes = taggingTypes();
  if (!props.links) return;
  return Object.fromEntries(
    Object.entries(props.links).filter((t) => tTypes.includes(t[0]))
  );
});

const groupedLinks = computed(() => {
  const tTypes = groupedTypes();
  if (!props.links) return;
  return Object.entries(props.links)
    .filter((t) => tTypes.includes(t[0]))
    .map((t) => t[1]);
});
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
