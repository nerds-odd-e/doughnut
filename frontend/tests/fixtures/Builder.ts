abstract class Builder<T=any> {
  protected parentBuilder: Builder | undefined;

  protected childrenBuilders: Array<Builder>;

  constructor(parentBuilder?: Builder) {
    this.parentBuilder = parentBuilder;
    this.childrenBuilders = [];
  }

  get and(): Builder {
    if (this.parentBuilder == null) {
      throw new Error("There is no parent builder");
    }
    return this.parentBuilder;
  }

  please(): T {
    if (this.parentBuilder !== undefined) {
      return this.parentBuilder.please();
    }
    return this.do();
  }

  abstract do(): T;
}

export default Builder;
