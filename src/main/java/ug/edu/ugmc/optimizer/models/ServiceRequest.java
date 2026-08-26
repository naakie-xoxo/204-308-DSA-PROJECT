package ug.edu.ugmc.optimizer.models;

/** Immutable service-request data loaded from the persistent SQLite store. */
public class ServiceRequest {

    /** Supported operational lifecycle states. */
    public enum Status {
        PENDING("Pending"),
        IN_PROGRESS("In Progress"),
        COMPLETED("Completed");

        private final String databaseValue;

        Status(String databaseValue) {
            this.databaseValue = databaseValue;
        }

        public String databaseValue() {
            return databaseValue;
        }

        public static Status fromDatabaseValue(String value) {
            if (value != null) {
                for (Status status : values()) {
                    if (status.databaseValue.equalsIgnoreCase(value.trim())) {
                        return status;
                    }
                }
            }
            throw new IllegalArgumentException("Unsupported request status: " + value);
        }
    }

    private final String id;
    private final String patientLabel;
    private final String sourceLocation;
    private final String destinationLocation;
    private final String category;
    private final int urgency;
    private final int weight;
    private final int value;
    private final String submitted;
    private final String deadline;
    private final Status status;

    public ServiceRequest(String id, int urgency) {
        this(id, urgency, 0, 0);
    }

    /** Compatibility constructor used by algorithm and data-structure tests. */
    public ServiceRequest(String id, int urgency, int weight, int value) {
        this(id, "", "", "", "", urgency, weight, value, "", "", Status.PENDING);
    }

    /** Creates the complete persistent request record used by examiner-facing lookup. */
    public ServiceRequest(
            String id,
            String patientLabel,
            String sourceLocation,
            String destinationLocation,
            String category,
            int urgency,
            int weight,
            int value,
            String submitted,
            String deadline,
            Status status) {
        this.id = requireText(id, "Request ID");
        this.patientLabel = nonNull(patientLabel);
        this.sourceLocation = nonNull(sourceLocation);
        this.destinationLocation = nonNull(destinationLocation);
        this.category = nonNull(category);
        this.urgency = urgency;
        this.weight = weight;
        this.value = value;
        this.submitted = nonNull(submitted);
        this.deadline = nonNull(deadline);
        if (status == null) {
            throw new IllegalArgumentException("Request status is required.");
        }
        this.status = status;
    }

    public ServiceRequest withStatus(Status newStatus) {
        return new ServiceRequest(
                id, patientLabel, sourceLocation, destinationLocation, category,
                urgency, weight, value, submitted, deadline, newStatus);
    }

    public String getId() { return id; }
    public String getPatientLabel() { return patientLabel; }
    public String getSourceLocation() { return sourceLocation; }
    public String getDestinationLocation() { return destinationLocation; }
    public String getCategory() { return category; }
    public int getUrgency() { return urgency; }
    public int getWeight() { return weight; }
    public int getValue() { return value; }
    public String getSubmitted() { return submitted; }
    public String getDeadline() { return deadline; }
    public Status getStatus() { return status; }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank.");
        }
        return value;
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}
