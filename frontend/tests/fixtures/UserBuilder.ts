import type { User } from "generated/backend/models/User"
import Builder from "./Builder"
import generateId from "./generateId"

class UserBuilder extends Builder<User> {
  data: User

  constructor() {
    super()
    this.data = {
      id: generateId(),
      name: "a name",
      externalIdentifier: `user ${generateId()}`,
      ownership: { id: 0 },
      dailyAssimilationCount: 5,
      spaceIntervals: "",
      admin: false,
    }
  }

  admin(isAdmin: boolean) {
    this.data.admin = isAdmin
    return this
  }

  do(): User {
    return this.data
  }
}

export default UserBuilder
