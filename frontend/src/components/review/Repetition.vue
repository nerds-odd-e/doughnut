<template>
  <StickTopBar v-if="compact">
    <div class="repeat-container" v-on:click="backToRepeat()">
      <SelfEvaluateButtons v-bind="{sadOnly}" @selfEvaluate="$emit('selfEvaluate', $event)"/>
    </div>
  </StickTopBar>
  <div v-else>
    <div v-if="answerResult">
        <div class="alert alert-success" v-if="answerResult.correct">Correct!</div>
        <div class="alert alert-danger" v-else>
              {{'Your answer `' + answerResult.answerDisplay + '` is wrong.'}}
        </div>
    </div>

    <ShowReviewPoint v-bind="{ noteViewedByUser, linkViewedByUser}" />
    <div class="btn-toolbar justify-content-between">
      <SelfEvaluateButtons v-bind="{sadOnly}" @selfEvaluate="$emit('selfEvaluate', $event)"/>
      <div class="btn-group dropup">
          <button type="button" id="more-action-for-repeat" class="btn btn-light dropdown-toggle"
                  data-toggle="dropdown" aria-haspopup="true"
                  aria-expanded="false">
              <SvgCog/>
          </button>
          <div class="dropdown-menu dropdown-menu-right">
              <a class="dropdown-item"
                  :href="`/notes/${sourceNoteViewedByUser.note.id}/review_setting`">
                  <SvgReviewSetting/>
                  Edit Review Settings
              </a>
              <a class="dropdown-item" :href="`/links/${sourceNoteViewedByUser.note.id}/link`">
                  <SvgLinkNote/>
                  Make Link
                </a>
                <router-link class="dropdown-item"
                    :to="{name: 'noteEdit', params: {noteid: sourceNoteViewedByUser.note.id}}">
                    <SvgEdit/>
                    Edit Note
                </router-link>
                <div class="dropdown-divider"></div>
                <form :action="`/reviews/${reviewPoint.id}`" method="post"
                      onsubmit="return confirm('Are you sure to hide this note from reviewing in the future?')">
                    <button type="submit" class="dropdown-item" name="remove">
                        <SvgNoReview/>
                        Remove This Note from Review
                    </button>
                </form>
          </div>
        </div>
    </div>
  </div>
  <RelativeRouterView v-bind="{staticInfo}"/>
</template>

<script>
  import SvgCog from "../svgs/SvgCog.vue"
  import SvgEdit from "../svgs/SvgEdit.vue"
  import SvgReviewSetting from "../svgs/SvgReviewSetting.vue"
  import SvgLinkNote from "../svgs/SvgLinkNote.vue"
  import SvgNoReview from "../svgs/SvgNoReview.vue"
  import ShowReviewPoint from "./ShowReviewPoint.vue"
  import StickTopBar from "../StickTopBar.vue"
  import SelfEvaluateButtons from "./SelfEvaluateButtons.vue"
  import RelativeRouterView from "../../routes/RelativeRouterView.vue"

  export default {
    name: "Repetition",
    props: {
      reviewPoint: { type: Object, required: true },
      answerResult: Object,
      noteViewedByUser: Object,
      linkViewedByUser: Object,
      staticInfo: Object,
      compact: Boolean
    },
    emits: ['selfEvaluate'],
    components: {SvgCog, SvgEdit, SvgReviewSetting, SvgLinkNote, SvgNoReview, ShowReviewPoint, StickTopBar, SelfEvaluateButtons, RelativeRouterView},
    computed: {
      sourceNoteViewedByUser() {
        if(!!this.noteViewedByUser) {
          return this.noteViewedByUser
        }
        else {
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
      backToRepeat(){
        this.$router.push({name: "repeat"})
      }
    }
  };
</script>

<style>
.repeat-container {
  background-color: rgba(50, 150, 50, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>
