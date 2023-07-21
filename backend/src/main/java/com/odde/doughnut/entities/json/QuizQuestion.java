package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.Thing;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.lang.Nullable;

@AllArgsConstructor
public class QuizQuestion {
  @Getter public Integer quizQuestionId;

  @Getter public String rawJsonQuestion;

  @Getter public QuizQuestionEntity.QuestionType questionType;

  @Getter public String description;

  @Getter public String mainTopic;

  @Getter public LinksOfANote hintLinks;

  @Getter public List<Integer> viceReviewPointIdList;

  @Getter @Nullable public NotePositionViewedByUser notebookPosition;

  @Getter public List<Option> options;

  @Getter public Optional<PictureWithMask> pictureWithMask;

  public static class Option {
    @Getter private Integer noteId;
    @Getter private boolean isPicture = false;
    @Getter private String display;
    @Getter @Nullable private PictureWithMask pictureWithMask;

    private Option() {}
  }

  public interface OptionCreator {
    Option optionFromThing(Thing thing);
  }

  public static class TitleOptionCreator implements OptionCreator {
    @Override
    public Option optionFromThing(Thing thing) {
      Option option = new Option();
      option.noteId = thing.getNote().getId();
      option.display = thing.getNote().getTitle();
      return option;
    }
  }

  public static class PictureOptionCreator implements OptionCreator {
    @Override
    public Option optionFromThing(Thing thing) {
      Option option = new Option();
      option.noteId = thing.getNote().getId();
      option.display = thing.getNote().getTitle();
      option.pictureWithMask = thing.getNote().getPictureWithMask().orElse(null);
      option.isPicture = true;
      return option;
    }
  }

  public static class ClozeLinkOptionCreator implements OptionCreator {
    @Override
    public Option optionFromThing(Thing thing) {
      Option option = new Option();
      option.noteId = thing.getLink().getSourceNote().getId();
      option.display = thing.getLink().getClozeSource().cloze();
      return option;
    }
  }
}
