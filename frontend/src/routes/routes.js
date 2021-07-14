import NotebooksPage from '@/pages/NotebooksPage.vue'
import NotebookNewPage from '@/pages/NotebookNewPage.vue'
import NoteShowPage from '@/pages/NoteShowPage.vue'
import LinkShowPage from '@/pages/LinkShowPage.vue'
import ReviewHome from '@/pages/ReviewHome.vue'
import RepeatPage from '@/pages/RepeatPage.vue'
import DoingQuiz from '@/pages/DoingQuiz.vue'
import InitialReviewPage from '@/pages/InitialReviewPage.vue'
import NestedPage from "@/pages/commons/NestedPage"

const NestedInitialReviewPage = NestedPage(
  InitialReviewPage,
  `initial`,
  [],
  `This will leave the initial review, are you sure?`,
  )

const NestedRepeatPage = NestedPage(
  RepeatPage,
  `repeat`,
  ['repeat-quiz'],
  `This will leave the current review, are you sure?`,
  )

const noteAndLinkRoutes = [
    { path: 'notebooks', name: 'notebooks', component: NotebooksPage },
    { path: 'notes/:noteId', name: 'noteShow', component: NoteShowPage, props: true },
    { path: 'links/:linkid', name: 'linkShow', component: LinkShowPage, props: true },
  ]
  
const nestedNoteAndLinkRoutes = (prefix) => noteAndLinkRoutes.map(route=>({...route, name: prefix + route.name}))

const routes = [
    ...noteAndLinkRoutes.map(route=>({...route, path: `/${route.path}`})),
    { path: '/', name: 'root', component: ReviewHome },
    { path: '/notebooks/new', name: 'notebookNew', component: NotebookNewPage },
    { path: '/bazaar/notes/:noteId', name: 'bnoteShow', component: NoteShowPage, props: true },
    { path: '/reviews', name: 'reviews', component: ReviewHome },
    { path: '/reviews/initial', name: 'initial', component: NestedInitialReviewPage, children:
              nestedNoteAndLinkRoutes('initial-') },
    { path: '/reviews/repeat', name: 'repeat', component: NestedRepeatPage, children: [
              ...nestedNoteAndLinkRoutes('repeat-'),
              { path: 'quiz', name: 'repeat-quiz', component: DoingQuiz }] },
  ]

export { routes }