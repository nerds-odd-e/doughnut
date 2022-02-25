let idCounter = 1

const generateId = () => {
    return (idCounter++).toString();
};

class CommentBuilder {
    data: any

    constructor() {
        this.data = {
            id: generateId(),
            content: 'this is a comment',
            user: null
        }
    }

    content(value: String): CommentBuilder {
        this.data.content = value
        return this
    }

    fromUser(value: Number): CommentBuilder {
        this.data.user = {id: value}
        return this
    }

    please(): any {
        return this.data;
    }
}

export default CommentBuilder