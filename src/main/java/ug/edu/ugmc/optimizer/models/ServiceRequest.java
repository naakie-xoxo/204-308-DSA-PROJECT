package ug.edu.ugmc.optimizer.models;

public class ServiceRequest {
    private final String id;
    private final int urgency;
    private final int weight;
    private final int value;
    
    public ServiceRequest(String id, int urgency) {
        this(id, urgency, 0, 0);
    }

    /**
     * Creates a service request with the values used by the optimization
     * algorithms.
     *
     * @param id unique request identifier
     * @param urgency request urgency level
     * @param weight resource cost associated with the request
     * @param value optimization value associated with the request
     */
    public ServiceRequest(String id, int urgency, int weight, int value) {
        this.id = id;
        this.urgency = urgency;
        this.weight = weight;
        this.value = value;
    }
    
    public String getId() { return id; }
    public int getUrgency() { return urgency; }
    public int getWeight() { return weight; }
    public int getValue() { return value; }
}
