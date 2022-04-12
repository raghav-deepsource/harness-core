resource "google_logging_metric" "ce_failed_batch_job" {
  name        = join("_", [local.name_prefix, "ce_failed_batch_jobs"])
  description = "Number of failed batch jobs. Owner: CE"
  filter = join("\n", [
    local.filter_prefix,
    "jsonPayload.logger=\"io.harness.batch.processing.schedule.BatchJobRunner\"",
    "jsonPayload.message:\"Error while running batch job\"",
    "jsonPayload.harness.batchJobType!=\"CE_SEGMENT_CALL\""
  ])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type  = "INT64"
    labels {
      key         = "accountId"
      value_type  = "STRING"
      description = "CE accountId"
    }
    labels {
      key         = "batchJobType"
      value_type  = "STRING"
      description = "CE Batch Job Type"
    }
  }
  label_extractors = {
    "accountId" : "EXTRACT(jsonPayload.harness.accountId)",
    "batchJobType" : "EXTRACT(jsonPayload.harness.batchJobType)"
  }
}

resource "google_monitoring_alert_policy" "ce_failed_batch_job_alert_policy" {
  notification_channels = ((var.deployment == "prod" || var.deployment == "freemium" || var.deployment == "prod_failover")
  ? ["${local.slack_prod_channel}"]
  : ((var.deployment == "qa" || var.deployment == "qa_free" || var.deployment == "stress")
    ? ["${local.slack_qa_channel}"]
    : ["${local.slack_dev_channel}"]))

  display_name = join("_", [local.name_prefix, "ce_failed_batch_jobs"])
  combiner     = "OR"
  conditions {
    display_name = "ce_failed_batch_jobs_per_type_per_account"
    condition_threshold {
      threshold_value = 0
      filter          = "resource.type=\"k8s_container\" AND metric.type=\"logging.googleapis.com/user/${google_logging_metric.ce_failed_batch_job.id}\""
      duration        = "180s"
      comparison      = "COMPARISON_GT"
      aggregations {
        group_by_fields      = ["metric.labels.batchJobType", "metric.labels.accountId", "resource.label.project_id"]
        alignment_period     = "300s"
        per_series_aligner   = "ALIGN_SUM"
        cross_series_reducer = "REDUCE_SUM"
      }
    }
  }


}
