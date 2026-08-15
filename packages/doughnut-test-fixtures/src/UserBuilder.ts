import type { User } from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'

class UserBuilder extends Builder<User> {
  data: User

  constructor() {
    super()
    this.data = {
      id: generateId(),
      name: 'a name',
      externalIdentifier: `user ${generateId()}`,
      ownership: { id: 0 },
      dailyAssimilationCount: 5,
      healthRemoveEmptyFoldersDefault: false,
      admin: false,
    }
  }

  admin(isAdmin: boolean) {
    this.data.admin = isAdmin
    return this
  }

  name(value: string) {
    this.data.name = value
    return this
  }

  dailyAssimilationCount(value: number) {
    this.data.dailyAssimilationCount = value
    return this
  }

  healthRemoveEmptyFoldersDefault(value: boolean) {
    this.data.healthRemoveEmptyFoldersDefault = value
    return this
  }

  withoutHealthRemoveEmptyFoldersDefault() {
    delete this.data.healthRemoveEmptyFoldersDefault
    return this
  }

  do(): User {
    return this.data
  }
}

export default UserBuilder
