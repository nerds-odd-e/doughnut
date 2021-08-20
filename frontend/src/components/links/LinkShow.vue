<template>
    <div class="row">
        <div class="col-12 col-md-4 link-source">
           <LinkNoteShow :noteViewedByUser= "sourceNoteViewedByUser" @updated="$emit('updated')"/>
        </div>
        <div class="col-12 col-md-4 text-center">
            <slot />
        </div>
        <div class="col-12 col-md-4 link-target">
           <LinkNoteShow :noteViewedByUser= "targetNoteViewedByUser" @updated="$emit('updated')"/>
        </div>
    </div>

</template>

<script>
import LinkNoteShow from "./LinkNoteShow.vue"

export default {
  props: {
    sourceNoteViewedByUser: {tyep: Object, required: true },
    targetNoteViewedByUser: {type: Object, required: true }
  },
  components: { LinkNoteShow }
}

</script>
