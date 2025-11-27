export const opsConfig = () => {
  const alertEmail = process.env.OPS_ALERT_EMAIL || ''
  const queueAlertThreshold = Number(process.env.QUEUE_ALERT_THRESHOLD || 5)
  const queueAlertDebounceMs = Number(process.env.QUEUE_ALERT_DEBOUNCE_MS || 5 * 60 * 1000)
  const dlqSample = Number(process.env.QUEUE_DLQ_SAMPLE || 5)

  return {
    alertEmail,
    queueAlertThreshold,
    queueAlertDebounceMs,
    dlqSample,
  }
}
