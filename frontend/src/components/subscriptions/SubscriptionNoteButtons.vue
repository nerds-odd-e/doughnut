<template>
<div class="btn-group btn-group-sm">
    <SubscriptionEditButton :subscription="subscription" />
    <form :action="`/subscriptions/${subscription.id}/delete`" method="post"
          onsubmit="return confirm('Are you sure to unsubscribe from this notebook??')">
        <button class="btn btn-sm" title="Unsubscribe">
            <SvgUnsubscribe/>
        </button>
    </form>
</div>

</template>

<script setup>
import SubscriptionEditButton from "./SubscriptionEditButton.vue"
import SvgUnsubscribe from "../svgs/SvgUnsubscribe.vue"
const props = defineProps({ subscription: Object })
</script>
