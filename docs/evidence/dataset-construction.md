# Dataset Construction and Local-Context Evidence

## Context and purpose

This project models service operations at the University of Ghana Medical
Centre (UGMC) in Ghana. It is a **UGMC-grounded computational simulation** for
the Data Structures and Algorithms project, not an official UGMC operational
database or floor plan. Real hospital terminology, UGMC departments and
public-facing facility information anchor the scenario, while modelling data
was added to provide enough records and relationships for algorithms,
database integration and empirical analysis.

The 50-location seed file includes locally meaningful hospital locations such
as UGMC Main Reception, Outpatient Department (OPD), Main Pharmacy, Triage
Area, Accident & Emergency (A&E), Medical Training & Simulation Centre,
Imaging & Radiology, Central Laboratory, Intensive Care Unit (ICU) and
Neonatal Intensive Care Unit (NICU). These names make the scenario recognisably
grounded in a Ghanaian hospital context. However, not every row in
`data/locations.csv` should be interpreted as an official UGMC room name or an
official UGMC floor-plan location. Labels such as block names and other
realistic operational locations were included for the simulation where the
repository does not preserve evidence that they are official UGMC labels.

## Dataset composition

The verified seed datasets contain:

- **50 locations** in `data/locations.csv`;
- **100 undirected road connections** in `data/roads.csv`;
- **300 service requests** in `data/service_requests.csv`; and
- **30 operational resources** in `data/resources.csv`.

The modelling additions serve specific assignment needs: graph algorithms
need a connected network with enough vertices and edges; queueing, searching
and sorting need a larger request set; scheduling needs assignable resources;
greedy and dynamic-programming optimisation need request weights and values;
and empirical experiments need repeatable input data. UGMC is not claimed to
have supplied these four project datasets.

## Local modelling coordinates

The `xCoordinate` and `yCoordinate` fields are positions on an artificial
local X/Y grid used solely for graph modelling. They are not GPS coordinates,
survey measurements or a representation of UGMC's exact internal geography.
For example, `LOC002` is modelled at `(50, 0)` and `LOC003` at `(60, 10)`.
These values make relative distances reproducible without claiming access to
an official site plan.

## Road construction and reproducibility

`RoadGenerator` is a dataset-construction utility, not the Dijkstra or routing
algorithm. Its implementation:

1. reads location IDs and local X/Y coordinates from `locations.csv`;
2. first connects consecutive locations in a ring, guaranteeing that the
   resulting undirected graph is connected;
3. adds unique, non-self location pairs until the dataset contains exactly
   100 roads;
4. calculates each distance with the Euclidean formula through `Math.hypot`;
5. derives travel time as at least one unit from the distance and speed
   modifier; and
6. generates a road-condition weight before writing the records to
   `roads.csv`.

The source preserves three index-associated reproducibility parameters:

- random seed: `22040372`;
- speed modifier: `7`; and
- congestion penalty: `2`.

The fixed random seed means that generation produces the same additional road
pairs and random condition components when the same ordered location input is
used. The repository labels these constants as derived from team index
numbers, but it does not preserve a derivation formula, so none is asserted
here.

## Service-request dataset evolution

The repository documents a migration from a legacy request layout containing
`requestId`, `sourceId`, `destinationId`, `category`, `urgency`,
`timeSubmitted`, `deadline` and `status`. Later optimisation work required the
expanded snake-case layout, including `patient_name`, `weight` and `value`.

`CsvMigrator` is a one-time data-preparation utility. It reads and validates an
existing legacy CSV, preserves the original request ID, source, destination,
category, urgency, submission time, deadline and status, creates a synthetic
label such as `Patient_REQ-001`, and generates the request `weight` and `value`
fields used by optimisation algorithms. It then writes the expanded layout.
The utility does **not** prove that it originally generated the complete
300-request dataset. The current repository preserves the migration process
from the earlier request format, but it does not contain enough evidence to
attribute every original legacy request row to a specific manual or automated
generation step.

## Resources and privacy

`data/resources.csv` models simulated hospital-operational assets, including
ambulances, rapid-response vehicles, mobile X-ray equipment, portable
ultrasound, defibrillators, crash carts and ventilators. Each record provides a
resource ID, type, home location, capacity and availability status. These rows
support the project's resource and scheduling model; the repository does not
preserve the exact original selection method for every resource row.

The project uses synthetic patient labels such as `Patient_REQ-001` and
simulated operational request records. It does not rely on real
patient-identifying information, and it does not claim that real patient data
was anonymised.

## Seed data and running database

The CSV files are the repository's seed inputs. During application startup,
the seed data is imported into the persistent SQLite database; the application
then loads persisted requests and graph records into project-owned in-memory
data structures for processing. This distinguishes the reviewable seed data
from the database used by the running application.

## Provenance limits

The repository does not establish:

- the exact original manual or automated generation method for every legacy
  service-request row;
- the exact original selection process for every resource row;
- that every location name is an official UGMC facility label; or
- that the local X/Y coordinates are surveyed positions or GPS coordinates.

This dataset should therefore be understood as a UGMC-grounded computational
simulation rather than an official UGMC operational dataset or floor plan.
Real hospital context was used to make the scenario locally meaningful, while
simulated coordinates, routes and operational records were added to satisfy
the project's algorithm, database and empirical-analysis requirements. No
real patient-identifying information is included.
