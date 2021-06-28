import NotebookNewPage from '@/pages/NotebookNewPage.vue'
import NoteShowPage from '@/pages/NoteShowPage.vue'
import NoteNewPage from '@/pages/NoteNewPage.vue'
import NoteEditPage from '@/pages/NoteEditPage.vue'
import LinkShowPage from '@/pages/LinkShowPage.vue'
import ReviewHome from '@/pages/ReviewHome.vue'
import Repeat from '@/pages/Repeat.vue'
import InitialReview from '@/pages/InitialReview.vue'

const noteAndLinkRoutes = [
    { path: 'notes/:noteid', name: 'noteShow', component: NoteShowPage, props: true },
    { path: 'notes/:noteid/new', name: 'noteNew', component: NoteNewPage, props: true },
    { path: 'notes/:noteid/edit', name: 'noteEdit', component: NoteEditPage, props: true },
    { path: 'links/:linkid', name: 'linkShow', component: LinkShowPage, props: true },
  ]
  
const nestedNoteAndLinkRoutes = (prefix) => noteAndLinkRoutes.map(route=>({...route, name: prefix + route.name}))

const routes = [
    ...noteAndLinkRoutes.map(route=>({...route, path: `/${route.path}`})),
    { path: '/', name: 'root', component: ReviewHome },
    { path: '/notebooks/new', name: 'notebookNew', component: NotebookNewPage },
    { path: '/bazaar/notes/:noteid', name: 'bnoteShow', component: NoteShowPage, props: true },
    { path: '/reviews', name: 'reviews', component: ReviewHome },
    { path: '/reviews/initial', name: 'initial', component: InitialReview },
    { path: '/reviews/repeat', name: 'repeat', component: Repeat, children: nestedNoteAndLinkRoutes('repeat-') },
  ]

export {routes}