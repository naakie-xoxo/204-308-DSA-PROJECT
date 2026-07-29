-- This creates the Locations table to hold hospital departments/wards
CREATE TABLE IF NOT EXISTS locations (
    locationId VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100),
    area VARCHAR(100),
    type VARCHAR(50),
    xCoordinate REAL,
    yCoordinate REAL
);
CREATE TABLE IF NOT EXISTS roads (
    fromLocationId VARCHAR(50),
    toLocationId VARCHAR(50),
    distance DECIMAL(5,2),
    travelTime INT,
    roadConditionWeight INT,
    PRIMARY KEY (fromLocationId, toLocationId),
    FOREIGN KEY (fromLocationId) REFERENCES locations(locationId),
    FOREIGN KEY (toLocationId) REFERENCES locations(locationId)
);

-- service_requests table (Min. 300 records)
CREATE TABLE IF NOT EXISTS service_requests (
    requestId VARCHAR(50) PRIMARY KEY,
    sourceId VARCHAR(50),
    destinationId VARCHAR(50),
    category VARCHAR(50),
    urgency INT,
    timeSubmitted VARCHAR(50),
    deadline VARCHAR(50),
    status VARCHAR(50),
    FOREIGN KEY (sourceId) REFERENCES locations(locationId),
    FOREIGN KEY (destinationId) REFERENCES locations(locationId)
);

-- resources table (Min. 30 records)
CREATE TABLE IF NOT EXISTS resources (
    resourceId VARCHAR(50) PRIMARY KEY,
    type VARCHAR(50),
    homeLocation VARCHAR(50),
    capacity INT,
    availabilityStatus VARCHAR(50),
    FOREIGN KEY (homeLocation) REFERENCES locations(locationId)
);

-- algorithm_runs table (To hold your team's efficiency lab results)
CREATE TABLE IF NOT EXISTS algorithm_runs (
    runId INTEGER PRIMARY KEY AUTOINCREMENT,
    algorithmName VARCHAR(50),
    inputSize INT,
    timeNs BIGINT,
    memoryKb BIGINT,
    dateRun VARCHAR(50)
);

-- audit_events table (For your stack-based undo operations)
CREATE TABLE IF NOT EXISTS audit_events (
    eventId INTEGER PRIMARY KEY AUTOINCREMENT,
    actionType VARCHAR(50),
    details TEXT,
    timestamp VARCHAR(50)
);