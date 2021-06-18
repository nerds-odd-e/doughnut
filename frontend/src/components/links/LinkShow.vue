<template>
    <div class="row">
        <div class="col-12 col-md-4">
           <LinkNoteShow :noteViewedByUser= "sourceNoteViewedByUser"/>
        </div>
        <div class="col-12 col-md-4 text-center">
            <slot />
        </div>
        <div class="col-12 col-md-4">
           <LinkNoteShow :noteViewedByUser= "targetNoteViewedByUser"/>
        </div>
    </div>

</template>

<script setup>
import LinkNoteShow from "./LinkNoteShow.vue"
import { ref, defineProps } from "vue"

  const props = defineProps({ sourceNoteViewedByUser: Object, targetNoteViewedByUser: Object })
</script>
