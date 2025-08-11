package live.alinmiron.beamerparts.vehicle.entity;

/**
 * Vehicle sync event processing status enumeration
 */
public enum VehicleSyncEventStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED,
    RETRYING
}
