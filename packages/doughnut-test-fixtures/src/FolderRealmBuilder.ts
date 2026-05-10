import type {
  FolderRealm,
  NotebookClientView,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NotebookBuilder from './NotebookBuilder'

class FolderRealmBuilder extends Builder<FolderRealm> {
  private notebookBuilder = new NotebookBuilder()
  private folderId = 1
  private folderName = 'Folder'

  folder(id: number, name: string): FolderRealmBuilder {
    this.folderId = id
    this.folderName = name
    return this
  }

  do(): FolderRealm {
    const now = new Date().toISOString()
    const notebook = this.notebookBuilder.do()
    const notebookView: NotebookClientView = {
      notebook,
      readonly: false,
    }
    return {
      notebookView,
      ancestorFolders: [],
      folder: {
        id: this.folderId,
        name: this.folderName,
        createdAt: now,
        updatedAt: now,
      },
    }
  }
}

export default FolderRealmBuilder
