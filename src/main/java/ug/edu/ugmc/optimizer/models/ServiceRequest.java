package ug.edu.ugmc.optimizer.models;

public class ServiceRequest {
    private String id;
    private int priority;
    
    public ServiceRequest(String id, int priority) {
        this.id = id;
        this.priority = priority;
    }
    
    public String getId() { return id; }
    public int getPriority() { return priority; }
}