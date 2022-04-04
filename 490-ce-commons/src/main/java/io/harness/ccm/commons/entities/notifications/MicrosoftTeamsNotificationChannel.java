package io.harness.ccm.commons.entities.notifications;

import static io.harness.notification.NotificationChannelType.MSTEAMS;

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
@JsonTypeName("MICROSOFT_TEAMS")
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "EmailNotificationChannelKeys")
public class MicrosoftTeamsNotificationChannel implements CCMNotificationChannel {
  String microsoftTeamsUrl;

  @Override
  public NotificationChannelType getNotificationChannelType() {
    return MSTEAMS;
  }

  @Override
  public List<String> getChannelUrls() {
    return Collections.singletonList(microsoftTeamsUrl);
  }
}
