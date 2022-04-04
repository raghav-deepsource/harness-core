package io.harness.ccm.commons.entities.notifications;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "CCMPerspectiveNotificationChannels",
    description =
        "This object contains details of Notification Channels set to receive Cloud cost anomaly alerts for a Perspective")
public class CCMPerspectiveNotificationChannelsDTO {
  String perspectiveId;
  String perspectiveName;
  List<CCMNotificationChannel> channels;
}
