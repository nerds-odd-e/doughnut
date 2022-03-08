import { defineStore } from "pinia";


interface State {
  notebooks: Generated.Notebook[]
  notes: {[id: number]: Generated.NoteViewedByUser }
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

    getChildrenIdsByParentId(parentId: number) {
      return !state.notes[parentId]
        ? []
        : state.notes[parentId].childrenIds
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
      const children = this.getNoteById(
        this.getNoteById(id)?.parentId
      )?.childrenIds
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
        loadNotes(notes: Generated.NoteViewedByUser[]) {
          notes.forEach((note) => {
            this.notes[note.id] = note;
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
