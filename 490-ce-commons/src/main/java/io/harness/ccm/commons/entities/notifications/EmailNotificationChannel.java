package io.harness.ccm.commons.entities.notifications;

import static io.harness.notification.NotificationChannelType.EMAIL;

import io.harness.notification.NotificationChannelType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@JsonTypeName("EMAIL")
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "EmailNotificationChannelKeys")
public class EmailNotificationChannel implements CCMNotificationChannel {
  List<String> emails;

  @Override
  public NotificationChannelType getNotificationChannelType() {
    return EMAIL;
  }

  @Override
  public List<String> getChannelUrls() {
    return emails;
  }
}
