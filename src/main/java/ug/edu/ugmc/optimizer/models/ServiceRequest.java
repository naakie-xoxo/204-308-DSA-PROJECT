package ug.edu.ugmc.optimizer.models;

public class ServiceRequest {
    private String id;
    private int urgency; 
    
    public ServiceRequest(String id, int urgency) {
        this.id = id;
        this.urgency = urgency;
    }
    
    public String getId() { return id; }
    public int getUrgency() { return urgency; } 
}