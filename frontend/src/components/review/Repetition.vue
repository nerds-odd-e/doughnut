<template>
  <template v-if="compact">
      <SelfEvaluateButtons v-bind="{sadOnly}" @selfEvaluate="$emit('selfEvaluate', $event)"/>
  </template>
  <template v-else>
    <div v-if="answerResult">
        <div class="alert alert-success" v-if="answerResult.correct">Correct!</div>
        <div class="alert alert-danger" v-else>
              {{'Your answer `' + answerResult.answerDisplay + '` is wrong.'}}
        </div>
    </div>

    <ShowReviewPoint v-bind="{ noteViewedByUser, linkViewedByUser}" @updated="$emit('updated')"/>
    <div class="btn-toolbar justify-content-between">
      <SelfEvaluateButtons v-bind="{sadOnly}" @selfEvaluate="$emit('selfEvaluate', $event)"/>
      <button class="btn" title="remove this note from review" @click="removeFromReview()">
          <SvgNoReview/>
      </button>
    </div>
  </template>
</template>

<script>
  import SvgCog from "../svgs/SvgCog.vue"
  import SvgNoReview from "../svgs/SvgNoReview.vue"
  import ShowReviewPoint from "./ShowReviewPoint.vue"
  import SelfEvaluateButtons from "./SelfEvaluateButtons.vue"
  import { restPost} from "../../restful/restful"

  export default {
    name: "Repetition",
    props: {
      reviewPoint: { type: Object, required: true },
      answerResult: Object,
      noteViewedByUser: Object,
      linkViewedByUser: Object,
      compact: Boolean
    },
    emits: ['selfEvaluate', 'updated'],
    components: {SvgCog, SvgNoReview, ShowReviewPoint, SelfEvaluateButtons },
    data() {
      return {
        loading: false
      }
    },
    computed: {
      sourceNoteViewedByUser() {
        if(!!this.noteViewedByUser) {
          return this.noteViewedByUser
        }
        if(this.linkViewedByUser){
          return this.linkViewedByUser.sourceNoteViewedByUser
        }
      },
      sadOnly() {
        if(!!this.answerResult) {
          return !this.answerResult.correct
        }
        return false
      },
    },
    methods: {
      async removeFromReview() {
        if (!await this.$popups.confirm(`Are you sure to hide this from reviewing in the future?`)) {
          return
        }
        restPost(
          `/api/review-points/${this.reviewPoint.id}/remove`,
          {},
          r=>this.loading = r)
          .then(r=>this.$emit('updated')
        )

      }

    }
  };
</script>
