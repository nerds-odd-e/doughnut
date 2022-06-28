<template>
  <NoteWithLinks
    v-bind="{ note: noteRealm.note, links: noteRealm.links }"
    @note-realm-updated="$emit('noteRealmUpdated', $event)"
  >
    <template #footer>
      <NoteStatisticsButton
        :note-id="noteRealm.id"
        :expanded="expandInfo"
        :key="noteRealm.id"
      />
    </template>
  </NoteWithLinks>
  <Cards v-if="expandChildren" :notes="noteRealm.children" />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteWithLinks from "../NoteWithLinks.vue";
import NoteStatisticsButton from "../NoteStatisticsButton.vue";
import Cards from "../Cards.vue";
import useStoredLoadingApi from "../../../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  props: {
    noteRealm: {
      type: Object as PropType<Generated.NoteRealm>,
      required: true,
    },
    expandChildren: { type: Boolean, required: true },
    expandInfo: { type: Boolean, required: false },
  },
  emits: ["noteRealmUpdated"],
  components: {
    NoteWithLinks,
    Cards,
    NoteStatisticsButton,
  },

  computed: {
    featureToggle() {
      return this.piniaStore.featureToggle;
    },
  },
});
</script>
