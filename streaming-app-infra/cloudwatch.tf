resource "aws_cloudwatch_dashboard" "jvm_metrics" {
  dashboard_name = "${var.environment}-jvm-metrics"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          title  = "JVM Heap Memory Used"
          region = var.aws_region
          metrics = [
            ["PageViewAggregator", "jvm.memory.used", "area", "heap", { stat = "Average", label = "Aggregator Heap Used" }],
            ["PageViewPublisher", "jvm.memory.used", "area", "heap", { stat = "Average", label = "Publisher Heap Used" }]
          ]
          view    = "timeSeries"
          stacked = false
          period  = 60
          yAxis = {
            left = { label = "Bytes" }
          }
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          title  = "JVM Heap Memory Max"
          region = var.aws_region
          metrics = [
            ["PageViewAggregator", "jvm.memory.max", "area", "heap", { stat = "Average", label = "Aggregator Heap Max" }],
            ["PageViewPublisher", "jvm.memory.max", "area", "heap", { stat = "Average", label = "Publisher Heap Max" }]
          ]
          view    = "timeSeries"
          stacked = false
          period  = 60
          yAxis = {
            left = { label = "Bytes" }
          }
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6
        properties = {
          title  = "JVM GC Pause Time"
          region = var.aws_region
          metrics = [
            ["PageViewAggregator", "jvm.gc.pause", { stat = "Average", label = "Aggregator GC Pause" }],
            ["PageViewPublisher", "jvm.gc.pause", { stat = "Average", label = "Publisher GC Pause" }]
          ]
          view    = "timeSeries"
          stacked = false
          period  = 60
          yAxis = {
            left = { label = "Seconds" }
          }
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 6
        width  = 12
        height = 6
        properties = {
          title  = "Process CPU Usage"
          region = var.aws_region
          metrics = [
            ["PageViewAggregator", "process.cpu.usage", { stat = "Average", label = "Aggregator CPU" }],
            ["PageViewPublisher", "process.cpu.usage", { stat = "Average", label = "Publisher CPU" }]
          ]
          view    = "timeSeries"
          stacked = false
          period  = 60
          yAxis = {
            left = { label = "Usage", max = 1 }
          }
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 12
        width  = 12
        height = 6
        properties = {
          title  = "JVM Threads (Aggregator)"
          region = var.aws_region
          metrics = [
            ["PageViewAggregator", "jvm.threads.live", { stat = "Average", label = "Live" }],
            ["PageViewAggregator", "jvm.threads.daemon", { stat = "Average", label = "Daemon" }],
            ["PageViewAggregator", "jvm.threads.peak", { stat = "Maximum", label = "Peak" }]
          ]
          view    = "timeSeries"
          stacked = false
          period  = 60
          yAxis = {
            left = { label = "Count" }
          }
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 12
        width  = 12
        height = 6
        properties = {
          title  = "JVM Threads (Publisher)"
          region = var.aws_region
          metrics = [
            ["PageViewPublisher", "jvm.threads.live", { stat = "Average", label = "Live" }],
            ["PageViewPublisher", "jvm.threads.daemon", { stat = "Average", label = "Daemon" }],
            ["PageViewPublisher", "jvm.threads.peak", { stat = "Maximum", label = "Peak" }]
          ]
          view    = "timeSeries"
          stacked = false
          period  = 60
          yAxis = {
            left = { label = "Count" }
          }
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 18
        width  = 12
        height = 6
        properties = {
          title  = "Connector JVM Heap Memory"
          region = var.aws_region
          metrics = [
            ["PageViewConnector", "jvm.memory.heap.used", { stat = "Average", label = "Heap Used" }],
            ["PageViewConnector", "jvm.memory.heap.max", { stat = "Average", label = "Heap Max" }],
            ["PageViewConnector", "jvm.memory.heap.committed", { stat = "Average", label = "Heap Committed" }]
          ]
          view    = "timeSeries"
          stacked = false
          period  = 60
          yAxis = {
            left = { label = "Bytes" }
          }
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 18
        width  = 12
        height = 6
        properties = {
          title  = "Connector JVM GC Collections"
          region = var.aws_region
          metrics = [
            ["PageViewConnector", "jvm.gc.collections.count", { stat = "Sum", label = "GC Count" }],
            ["PageViewConnector", "jvm.gc.collections.elapsed", { stat = "Average", label = "GC Elapsed" }]
          ]
          view    = "timeSeries"
          stacked = false
          period  = 60
          yAxis = {
            left = { label = "Count / Seconds" }
          }
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 24
        width  = 12
        height = 6
        properties = {
          title  = "Connector JVM Threads"
          region = var.aws_region
          metrics = [
            ["PageViewConnector", "jvm.threads.count", { stat = "Average", label = "Thread Count" }]
          ]
          view    = "timeSeries"
          stacked = false
          period  = 60
          yAxis = {
            left = { label = "Count" }
          }
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 24
        width  = 12
        height = 6
        properties = {
          title  = "Connector CPU Usage"
          region = var.aws_region
          metrics = [
            ["PageViewConnector", "jvm.cpu.recent_utilization", { stat = "Average", label = "CPU Usage" }]
          ]
          view    = "timeSeries"
          stacked = false
          period  = 60
          yAxis = {
            left = { label = "Usage", max = 1 }
          }
        }
      }
    ]
  })
}
