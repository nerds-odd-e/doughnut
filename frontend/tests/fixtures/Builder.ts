abstract class Builder<T> {
  parent<P extends Builder<S>, S>(parentBuilder: P) {
    const that = this as Omit<typeof this, "please">;
    const those = that as typeof that & {
      please: () => S;
      and: P;
    };
    those.and = parentBuilder;
    those.please = () => parentBuilder.please();
    return those;
  }

  please(): T {
    return this.do();
  }

  abstract do(): T;
}

export default Builder;
