class Note {
  final int id;
  final bool skipReviewEntirely;

  Note({this.id = 0, this.skipReviewEntirely = false});

  factory Note.fromJson(Map<String, dynamic> json) {
    return Note(
      id: json['id'],
      skipReviewEntirely: json['skipReviewEntirely'],
    );
  }
}
