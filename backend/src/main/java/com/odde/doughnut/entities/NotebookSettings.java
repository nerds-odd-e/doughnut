package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.odde.doughnut.entities.converters.PeriodConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.io.IOException;
import java.time.Period;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Embeddable
public class NotebookSettings {
  @Column(name = "skip_memory_tracking_entirely")
  Boolean skipMemoryTrackingEntirely = false;

  @Column(name = "number_of_questions_in_assessment")
  Integer numberOfQuestionsInAssessment;

  @Column(name = "certificate_expiry")
  @Convert(converter = PeriodConverter.class)
  @Schema(implementation = String.class, defaultValue = "1y", example = "1y 6m")
  @JsonSerialize(using = PeriodSerializer.class)
  @JsonDeserialize(using = PeriodDeserializer.class)
  @Getter
  @Setter
  Period certificateExpiry = Period.ofYears(1);

  @JsonIgnore
  public void update(NotebookSettings value) {
    setSkipMemoryTrackingEntirely(value.getSkipMemoryTrackingEntirely());
    setNumberOfQuestionsInAssessment(value.getNumberOfQuestionsInAssessment());
    setCertificateExpiry(value.getCertificateExpiry());
  }

  static class PeriodSerializer extends JsonSerializer<Period> {
    @Override
    public void serialize(Period value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeString(
          value.toString().substring(1).toLowerCase().replaceAll("([a-z]+)", "$1 ").trim());
    }
  }

  static class PeriodDeserializer extends JsonDeserializer<Period> {
    @Override
    public Period deserialize(JsonParser p, DeserializationContext context) throws IOException {
      var stringVal = p.getValueAsString().toUpperCase().replaceAll(" ", "");
      return Period.parse("P" + stringVal);
    }
  }
}
