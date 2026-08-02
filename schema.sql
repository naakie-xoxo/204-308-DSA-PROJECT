PRAGMA foreign_keys = ON;

-- Canonical schema for the UGMC Smart Service Operations Optimizer.
CREATE TABLE IF NOT EXISTS locations (
    locationId TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    area TEXT NOT NULL,
    type TEXT NOT NULL,
    xCoordinate REAL NOT NULL,
    yCoordinate REAL NOT NULL
);

-- Each row represents one undirected road. Graph loaders add both directions.
CREATE TABLE IF NOT EXISTS roads (
    fromLocationId TEXT NOT NULL,
    toLocationId TEXT NOT NULL,
    distance REAL NOT NULL CHECK (distance > 0),
    travelTime INTEGER NOT NULL CHECK (travelTime > 0),
    roadConditionWeight INTEGER NOT NULL CHECK (roadConditionWeight > 0),
    PRIMARY KEY (fromLocationId, toLocationId),
    CHECK (fromLocationId <> toLocationId),
    FOREIGN KEY (fromLocationId) REFERENCES locations(locationId),
    FOREIGN KEY (toLocationId) REFERENCES locations(locationId)
);

CREATE TABLE IF NOT EXISTS service_requests (
    requestId TEXT PRIMARY KEY,
    sourceId TEXT NOT NULL,
    destinationId TEXT NOT NULL,
    category TEXT NOT NULL,
    urgency INTEGER NOT NULL CHECK (urgency BETWEEN 1 AND 5),
    timeSubmitted TEXT NOT NULL,
    deadline TEXT NOT NULL,
    status TEXT NOT NULL,
    CHECK (sourceId <> destinationId),
    FOREIGN KEY (sourceId) REFERENCES locations(locationId),
    FOREIGN KEY (destinationId) REFERENCES locations(locationId)
);

CREATE TABLE IF NOT EXISTS resources (
    resourceId TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    homeLocation TEXT NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    availabilityStatus TEXT NOT NULL,
    FOREIGN KEY (homeLocation) REFERENCES locations(locationId)
);

CREATE TABLE IF NOT EXISTS algorithm_runs (
    runId INTEGER PRIMARY KEY,
    algorithmName TEXT NOT NULL,
    inputSize INTEGER NOT NULL CHECK (inputSize >= 0),
    timeNs INTEGER NOT NULL CHECK (timeNs >= 0),
    memoryKb INTEGER NOT NULL CHECK (memoryKb >= 0),
    dateRun TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_events (
    eventId INTEGER PRIMARY KEY AUTOINCREMENT,
    actionType TEXT NOT NULL,
    details TEXT NOT NULL,
    timestamp TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_requests_status_urgency
    ON service_requests(status, urgency DESC);
CREATE INDEX IF NOT EXISTS idx_requests_source
    ON service_requests(sourceId);
CREATE INDEX IF NOT EXISTS idx_requests_destination
    ON service_requests(destinationId);
CREATE INDEX IF NOT EXISTS idx_resources_home
    ON resources(homeLocation);
