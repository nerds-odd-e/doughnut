<template>
  <div class="row">
    <div class="col-12 col-md-4 link-source">
      <LinkNoteShow :note="link.sourceNote" />
    </div>
    <div class="col-12 col-md-4 text-center">
      <LinkNob v-bind="{ link, storageAccessor }" />
      <span class="badge bg-light text-dark"> {{ link.linkType }}</span>
    </div>
    <div class="col-12 col-md-4 link-target">
      <LinkNoteShow :note="link.targetNote" />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkNoteShow from "./LinkNoteShow.vue";
import LinkNob from "./LinkNob.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    link: {
      type: Object as PropType<Generated.Link>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { LinkNoteShow, LinkNob },
});
</script>
