PRAGMA foreign_keys = ON;

-- Canonical schema for the UGMC Smart Service Operations Optimizer.
CREATE TABLE IF NOT EXISTS locations (
    location_id TEXT PRIMARY KEY,
    location_name TEXT NOT NULL,
    area TEXT NOT NULL,
    type TEXT NOT NULL,
    x_coordinate REAL NOT NULL,
    y_coordinate REAL NOT NULL
);

-- Each row represents one undirected road. Graph loaders add both directions.
CREATE TABLE IF NOT EXISTS roads (
    source_id TEXT NOT NULL,
    destination_id TEXT NOT NULL,
    distance REAL NOT NULL CHECK (distance > 0),
    travel_time INTEGER NOT NULL CHECK (travel_time > 0),
    road_condition_weight INTEGER NOT NULL CHECK (road_condition_weight > 0),
    PRIMARY KEY (source_id, destination_id),
    CHECK (source_id <> destination_id),
    FOREIGN KEY (source_id) REFERENCES locations(location_id),
    FOREIGN KEY (destination_id) REFERENCES locations(location_id)
);

CREATE TABLE IF NOT EXISTS service_requests (
    request_id TEXT PRIMARY KEY,
    patient_name TEXT NOT NULL,
    source_id TEXT NOT NULL,
    destination_id TEXT NOT NULL,
    category TEXT NOT NULL,
    urgency_level INTEGER NOT NULL CHECK (urgency_level BETWEEN 1 AND 5),
    weight INTEGER NOT NULL,
    value INTEGER NOT NULL,
    time_submitted TEXT NOT NULL,
    deadline TEXT NOT NULL,
    status TEXT NOT NULL,
    CHECK (source_id <> destination_id),
    FOREIGN KEY (source_id) REFERENCES locations(location_id),
    FOREIGN KEY (destination_id) REFERENCES locations(location_id)
);

CREATE TABLE IF NOT EXISTS resources (
    resource_id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    home_location TEXT NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    availability_status TEXT NOT NULL,
    FOREIGN KEY (home_location) REFERENCES locations(location_id)
);

CREATE TABLE IF NOT EXISTS algorithm_runs (
    run_id INTEGER PRIMARY KEY,
    algorithm_name TEXT NOT NULL,
    input_size INTEGER NOT NULL CHECK (input_size >= 0),
    time_ns INTEGER NOT NULL CHECK (time_ns >= 0),
    memory_kb INTEGER NOT NULL CHECK (memory_kb >= 0),
    date_run TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_events (
    event_id INTEGER PRIMARY KEY AUTOINCREMENT,
    action_type TEXT NOT NULL,
    details TEXT NOT NULL,
    timestamp TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_requests_status_urgency
    ON service_requests(status, urgency_level DESC);
CREATE INDEX IF NOT EXISTS idx_requests_source
    ON service_requests(source_id);
CREATE INDEX IF NOT EXISTS idx_requests_destination
    ON service_requests(destination_id);
CREATE INDEX IF NOT EXISTS idx_resources_home
    ON resources(home_location);
