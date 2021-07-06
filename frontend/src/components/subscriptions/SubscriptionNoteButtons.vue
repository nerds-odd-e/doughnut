<template>
<div class="btn-group btn-group-sm">
    <a class="btn btn-sm" role="button" :href="`/subscriptions/${subscription.id}/edit`"
       title="Edit subscription">
        <SvgEdit/>
    </a>
    <form :action="`/subscriptions/${subscription.id}/delete`" method="post"
          onsubmit="return confirm('Are you sure to unsubscribe from this notebook??')">
        <button class="btn btn-sm" title="Unsubscribe">
            <SvgUnsubscribe/>
        </button>
    </form>
</div>

</template>

<script>
import SvgEdit from "../svgs/SvgEdit.vue"
import SvgUnsubscribe from "../svgs/SvgUnsubscribe.vue"
export default {
  name: 'NotebookCardWithHeaderAndButtons',
  props: { subscription: Object },
  components: { SvgEdit, SvgUnsubscribe },
}
</script>
