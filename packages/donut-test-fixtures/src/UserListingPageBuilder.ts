import type {
  UserListingPage,
  UserForListing,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'

export default class UserListingPageBuilder extends Builder<UserListingPage> {
  data: UserListingPage

  constructor() {
    super()
    this.data = {
      users: [],
      pageIndex: 0,
      pageSize: 10,
      totalCount: 0,
      totalPages: 0,
    }
  }

  do(): UserListingPage {
    return this.data
  }

  withUsers(users: UserForListing[]): this {
    this.data.users = users
    this.data.totalCount = users.length
    this.data.totalPages = Math.ceil(users.length / (this.data.pageSize || 10))
    return this
  }

  withPageIndex(pageIndex: number): this {
    this.data.pageIndex = pageIndex
    return this
  }

  withPageSize(pageSize: number): this {
    this.data.pageSize = pageSize
    return this
  }

  withTotalCount(totalCount: number): this {
    this.data.totalCount = totalCount
    this.data.totalPages = Math.ceil(totalCount / (this.data.pageSize || 10))
    return this
  }
}
