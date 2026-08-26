import type { Folder } from '@generated/doughnut-backend-api'
import Builder from './Builder'

const DEFAULT_FOLDER_DATE_TIME = '2000-01-01T00:00:00.000Z'

class FolderBuilder extends Builder<Folder> {
  private folderId = 1
  private folderName = 'Folder'

  id(value: number): FolderBuilder {
    this.folderId = value
    return this
  }

  name(value: string): FolderBuilder {
    this.folderName = value
    return this
  }

  folder(id: number, name: string): FolderBuilder {
    this.folderId = id
    this.folderName = name
    return this
  }

  do(): Folder {
    return {
      id: this.folderId,
      name: this.folderName,
      createdAt: DEFAULT_FOLDER_DATE_TIME,
      updatedAt: DEFAULT_FOLDER_DATE_TIME,
    }
  }
}

export default FolderBuilder
