import { defineStore } from "pinia";


interface NoteCacheState {
  notebooks: Generated.NotebookViewedByUser[]
  notebooksMapByHeadNoteId: {[id: Doughnut.ID]: Generated.NotebookViewedByUser}
  noteSpheres: {[id: Doughnut.ID]: Generated.NoteSphere }
}

interface State extends NoteCacheState {
  noteUndoHistories: any[]
  currentUser: Generated.User | null
  featureToggle: boolean
  environment: 'production' | 'testing'
}

function withState(state: NoteCacheState) {
  return {
    getNoteSphereById(id: Doughnut.ID | undefined) {
      if(id === undefined) return undefined;
      return state.noteSpheres[id]
    },

    getLinksById(id: Doughnut.ID) {
      return this.getNoteSphereById(id)?.links
    },

    getNotePosition(id: Doughnut.ID | undefined) {
      if(!id) return undefined
      const ancestors: Generated.Note[] = []
      let cursor = this.getNoteSphereById(id)
      while(cursor && cursor.note.parentId) {
        cursor = this.getNoteSphereById(cursor.note.parentId)
        if(!cursor) return undefined
        ancestors.unshift(cursor.note)
      }
      if(!cursor) return undefined
      const notebook = state.notebooksMapByHeadNoteId[cursor.id]
      return { noteId: id, ancestors, notebook } as Generated.NotePositionViewedByUser
    },

    getChildrenIdsByParentId(parentId: Doughnut.ID) {
      return this.getNoteSphereById(parentId)?.childrenIds
    },

    deleteNote(id: Doughnut.ID) {
      this.getChildrenIdsByParentId(id)?.forEach((cid: Doughnut.ID)=>this.deleteNote(cid))
      delete state.noteSpheres[id]
    },

    deleteNoteFromParentChildrenList(id: Doughnut.ID) {
      const parent = this.getNoteSphereById(id)?.note.parentId
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
        notebooksMapByHeadNoteId: {},
        noteSpheres: {},
        noteUndoHistories: [],
        currentUser: null,
        featureToggle: false,
        environment: 'production',
    } as State),

    getters: {
        getNoteSphereById: (state)        => (id: Doughnut.ID) => withState(state).getNoteSphereById(id),
        getNotePosition: (state)        => (id: Doughnut.ID) => withState(state).getNotePosition(id),
        getLinksById: (state)        => (id: Doughnut.ID) => withState(state).getLinksById(id),
        peekUndo: (state)           => () => {
          if(state.noteUndoHistories.length === 0) return null
          return state.noteUndoHistories[state.noteUndoHistories.length - 1]
        },
    },

    actions: {

        loadNotebooks(notebooks: Generated.NotebookViewedByUser[]) {
          this.notebooks = notebooks
          notebooks.forEach(nb=>{ this.loadNotebook(nb) })
        },

        loadNotebook(notebook: Generated.NotebookViewedByUser) {
          this.notebooksMapByHeadNoteId[notebook.headNoteId] = notebook
        },

        addEditingToUndoHistory({noteId}: {noteId: Doughnut.ID}) {
          this.noteUndoHistories.push({type: 'editing', noteId, textContent: {...withState(this).getNoteSphereById(noteId)?.note.textContent}});
        },
        popUndoHistory() {
          if (this.noteUndoHistories.length === 0) {
            return
          }
          this.noteUndoHistories.pop();
        },

        loadNoteSpheres(noteSpheres: Generated.NoteSphere[]) {
          noteSpheres.forEach((noteSphere) => {
            this.noteSpheres[noteSphere.id] = noteSphere;
          });
        },

        loadNote(id: Doughnut.ID, note: Generated.Note) {
          const noteSphere = this.noteSpheres[id];
          if (!noteSphere) {
            this.noteSpheres[id] = {id, note }
            return
          }
          noteSphere.note = note
        },


        loadNotePosition(notePosition: Generated.NotePositionViewedByUser) {
          notePosition.ancestors.forEach((note) => {
            const {id} = note;
            this.loadNote(id, note)
          });
          this.loadNotebook(notePosition.notebook)
        },

        loadNotesBulk(noteBulk: Generated.NotesBulk) {
          this.loadNoteSpheres(noteBulk.notes);
          this.loadNotePosition(noteBulk.notePosition);
        },

        loadNoteWithPosition(noteWithPosition: Generated.NoteWithPosition) {
          this.loadNoteSpheres([noteWithPosition.note]);
          this.loadNotePosition(noteWithPosition.notePosition);
        },

        deleteNote(noteId: Doughnut.ID) {
          withState(this).deleteNoteFromParentChildrenList(noteId)
          withState(this).deleteNote(noteId)
          this.noteUndoHistories.push({type: 'delete note', noteId});
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
