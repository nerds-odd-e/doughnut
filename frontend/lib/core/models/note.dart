class Note {
  int id = 0;
  bool skipReviewEntirely = false;

  Note({this.id = 0, this.skipReviewEntirely = false});

  Note.fromJson(Map<String, dynamic> json) {
    id = json['id'];
    skipReviewEntirely = json['skipReviewEntirely'];
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Note &&
          runtimeType == other.runtimeType &&
          id == other.id;

  @override
  int get hashCode => id.hashCode;
}
