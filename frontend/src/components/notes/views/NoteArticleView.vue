<template>
  <template v-if="noteRealm">
    <NoteWithLinks
      v-bind="{ note: noteRealm.note, links: noteRealm.links }"
      @note-realm-updated="$emit('noteRealmUpdated', $event)"
    />
    <div class="note-list">
      <NoteArticleView
        v-for="child in children"
        v-bind="{ noteRealm: child, expandChildren }"
        :key="child.id"
        @note-realm-updated="$emit('noteRealmUpdated', $event)"
      />
    </div>
  </template>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "../../../managedApi/useLoadingApi";
import NoteWithLinks from "../NoteWithLinks.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    noteRealm: {
      type: Object as PropType<Generated.NoteRealm>,
      required: true,
    },
    expandChildren: { type: Boolean, required: true },
  },
  components: { NoteWithLinks },
  emits: ["noteRealmUpdated"],
  data() {
    return {
      children: undefined as Generated.NoteRealm[] | undefined,
    };
  },
  methods: {
    async fetchChildren() {
      this.children = await this.api.getNoteRealmsByIds(
        this.noteRealm.children.map((child) => child.id)
      );
    },
  },
  mounted() {
    this.fetchChildren();
  },
});
</script>

<style lang="sass" scoped>

:deep(.note-body)
  border-width: 0px
  border-left-width: 3px

.note-list
  margin-left: 10px
</style>
