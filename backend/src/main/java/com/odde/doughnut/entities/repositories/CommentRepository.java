package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Comment;
import com.odde.doughnut.entities.Note;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface CommentRepository extends CrudRepository<Comment, Integer> {

    @Query(value = " Select comment.* from comment WHERE note_id = :note", nativeQuery = true)
    List<Comment> getCommentsByNote(@Param("note") Note note);

    @Modifying
    @Query(value = " UPDATE comment SET deleted_at = :currentUTCTimestamp WHERE id = :#{#comment.id}", nativeQuery = true)
    void softDelete(@Param("comment") Comment comment, @Param("currentUTCTimestamp") Timestamp currentUTCTimestamp);


    @Modifying
    @Query( value = " UPDATE comment SET content = :#{#comment.content} WHERE id = :#{#comment.id}", nativeQuery = true)
    void edit(@Param("comment")Comment comment);

}
