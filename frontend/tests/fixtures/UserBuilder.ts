import Builder from "./Builder";
import generateId from "./generateId";

class UserBuilder extends Builder<Generated.User> {
  data: Generated.User;

  constructor() {
    super();
    this.data = {
      id: generateId(),
      name: "a name",
      externalIdentifier: "an external identifier",
      ownership: { id: 0 },
      dailyNewNotesCount: 5,
      spaceIntervals: "",
      aiQuestionTypeOnlyForReview: false,
      admin: false,
    };
  }

  admin(isAdmin: boolean) {
    this.data.admin = isAdmin;
    return this;
  }

  do(): Generated.User {
    return this.data;
  }
}

export default UserBuilder;
