import { defineStore } from "pinia";
import MinimumNoteSphere from "./MinimumNoteSphere";
import Links from "./Links";

interface State {
  notebooks: Generated.Notebook[]
  notes: {[id: number]: Generated.Note }
  links: {[id: number]: Links }
  parentChildrenIds: {[id: number]: number[] }
  highlightNoteId: number | undefined
  noteUndoHistories: any[]
  currentUser: Generated.User | null
  featureToggle: boolean
  viewType: string
  environment: 'production' | 'testing'
}

function withState(state: State) {
  return {
    getNoteById(id: number | undefined) {
      if(id === undefined) return undefined;
      return state.notes[id]
    },
    getLinksById(id: number | undefined) {
      if(id === undefined) return undefined;
      return state.links[id]
    },

    getChildrenIdsByParentId(parentId: number) {
      return state.parentChildrenIds[parentId] || []
    },

    getChildrenOfParentId(parentId: number) {
      return this.getChildrenIdsByParentId(parentId)
        .map((id: number)=>this.getNoteById(id))
        .filter((n: any)=>n)
    },
   
    deleteNote(id: number) {
      this.getChildrenIdsByParentId(id)?.forEach((cid: number)=>this.deleteNote(cid))
      delete state.notes[id]
    },

    deleteNoteFromParentChildrenList(id: number) {
      const parent = this.getNoteById(id)?.parentId
      if(!parent) return
      const children = this.getChildrenIdsByParentId(parent)
      if (children) {
        const index = children.indexOf(id)
        if (index > -1) {
          children.splice(index, 1);
        }
      }
    },
  }
}

export default defineStore('main', {
    state: () => ({
        notebooks: [],
        notes: {},
        links: {},
        parentChildrenIds: {},
        highlightNoteId: undefined,
        noteUndoHistories: [],
        currentUser: null,
        featureToggle: false,
        viewType: 'card',
        environment: 'production',
    } as State),

    getters: {
        getHighlightNote: (state: State)   => () => withState(state).getNoteById(state.highlightNoteId),
        getNoteById: (state)        => (id: number) => withState(state).getNoteById(id),
        getNoteByIdLegacy: (state)        => (id: number): MinimumNoteSphere | undefined => {
          const note = withState(state).getNoteById(id)
          if(!note) return undefined
          const {parentId} = note
          const links = withState(state).getLinksById(id)
          const childrenIds = withState(state).getChildrenIdsByParentId(id)
          return {id, parentId, links, childrenIds }
        },
        getLinksById: (state)        => (id: number) => withState(state).getLinksById(id),
        peekUndo: (state)           => () => {
          if(state.noteUndoHistories.length === 0) return null
          return state.noteUndoHistories[state.noteUndoHistories.length - 1]
        },
        getChildrenIdsByParentId: (state) => (parentId: number) => withState(state).getChildrenIdsByParentId(parentId),
        getChildrenOfParentId: (state)    => (parentId: number) => withState(state).getChildrenOfParentId(parentId),
    },

    actions: {
        setNotebooks(notebooks: Generated.Notebook[]) {
          this.notebooks = notebooks
        },
        addEditingToUndoHistory({noteId}: {noteId: number}) {
          this.noteUndoHistories.push({type: 'editing', noteId, textContent: {...withState(this).getNoteById(noteId)?.textContent}});
        },
        popUndoHistory() {
          if (this.noteUndoHistories.length === 0) {
            return
          }
          this.noteUndoHistories.pop();
        },
        loadNoteSpheres(noteSpheres: Generated.NoteSphere[]) {

          noteSpheres.forEach((noteSphere) => {
            const {id} = noteSphere.note;
            this.notes[id] = noteSphere.note;
            this.links[id] = noteSphere.links;
            this.parentChildrenIds[id] = noteSphere.childrenIds;
          });
        },
        deleteNote(noteId: number) {
          withState(this).deleteNoteFromParentChildrenList(noteId)
          withState(this).deleteNote(noteId)
          this.noteUndoHistories.push({type: 'delete note', noteId});
        },
        setHighlightNoteId(noteId: number) {
          this.highlightNoteId = noteId
        },
        setViewType(viewType: string) {
          this.viewType = viewType
        },
        setCurrentUser(user: Generated.User) {
          this.currentUser = user
        },
        setFeatureToggle(ft: boolean) {
          this.environment = "testing"
          this.featureToggle = ft
        },
      },
    });
