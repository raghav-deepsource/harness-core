package io.harness.ccm.commons.entities.notifications;

import static io.harness.notification.NotificationChannelType.SLACK;

import io.harness.notification.NotificationChannelType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@JsonTypeName("SLACK")
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "SlackNotificationChannelKeys")
public class SlackNotificationChannel implements CCMNotificationChannel {
  String slackWebHookUrl;

  @Override
  public NotificationChannelType getNotificationChannelType() {
    return SLACK;
  }

  @Override
  public List<String> getChannelUrls() {
    return Collections.singletonList(slackWebHookUrl);
  }
}
