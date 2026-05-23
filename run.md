# IssueFlow - Run Guide

This file explains how to run, test, and manually validate the IssueFlow backend locally.


## Table of Contents

| Section | Description                       | 
|--------|------------------------------------| 
| 1      | Prerequisites                      |
| 2      | Start PostgreSQL                   | 
| 3      | Build the Project                  | 
| 4      | Run Automated Tests                | 
| 5      | Run the Application                |
| 6      | Authentication Flow                |
| 7      | Password Note                      | 
| 8      | Environment Variables              | 
| 9      | Manual Postman Test Artifacts      | 
| 10     | General Postman Setup              | 
| 11     | How to Use Each Postman Collection |
| 11.1   | Manual Smoke Tests                 | 
| 11.2   | Comments and Mentions Tests        |
| 11.3   | Ticket Dependencies Tests          |
| 11.4   | Auto-Assignment and Workload Tests |
| 11.5   | CSV Import/Export Tests            |
| 11.6   | Attachmentes Tests                 |
| 11.7   | Auto-Escalation Scheduler Tests    |
| 11.8   | Audit Log API Tests                |
| 11.9   | Final Smoke Regression Collection  |
| 12     | Troubleshooting                    |
| 13     | Additional Notes                   |







## 1. Prerequisites

Install:

- Java 21
- Docker Desktop
- Git
- Postman
- IntelliJ IDEA or another Java IDE

Verify Java:

powershell/cmd
java -version
javac -version


*Note If Java is installed but not detected in PowerShell/CMD, set JAVA_HOME in the current PowerShell session
meaning Adjust the path if your local JDK is installed somewhere else.


## 2. Start PostgreSQL
Via the IDE terminal from the project root write the following: 



docker compose down -v  (to make sure database is clean)  
docker compose up -d


Note - make sure Docker Desktop is running(or a similar app).

## 3. Build the Project
Via the IDE terminal from the project root write the following:

.\mvnw.cmd clean package



## 4. Run Automated Tests
Via the IDE terminal from the project root write the following:


.\mvnw.cmd clean test

Note At the time of final regression validation, you should expect to see : 106 passed, 0 failures, 0 errors



## 5. Run the Application

Via the IDE terminal from the project root write the following: 

docker compose down -v  (to make sure database is clean)  
docker compose up -d

Then run the Spring Boot application:

.\mvnw.cmd spring-boot:run

The backend runs on :http://localhost:8080


## 6. Authentication Flow

Create a user: (via postman or something similar)

POST http://localhost:8080/users
Content-Type: application/json


Example body:

{
"username": "admin1",
"email": "admin1@example.com",
"fullName": "Admin One",
"role": "ADMIN"
}


Login:

POST http://localhost:8080/auth/login
Content-Type: application/json

Example body:

{
"username": "admin1",
"password": "secret"
}

Use the returned token for protected endpoints:
Authorization: Bearer <accessToken> (in postman just make sure to put the token inside the correct tab, very simple)


All endpoints are JWT-protected except:
POST /users
POST /auth/login


Logout: 

POST http://localhost:8080/auth/logout
Authorization: Bearer <accessToken>

After logout, the token is revoked and should no longer work.


## 7. Password Note

The README.md given in the task, POST /users body does not include a password field, but login requires a password.

For compatibility with the provided API contract, users created without a password receive the default development password:

secret

The implementation also supports an optional password field in POST /users.

Passwords are always stored as BCrypt hashes and are never returned in API responses.



## 8. Environment Variables

JWT settings can be customized with environment variables:

JWT_SECRET
JWT_EXPIRATION_SECONDS

If not provided, local development defaults are used.

Example:
$env:JWT_SECRET="replace-this-with-a-long-secret-for-local-testing"
$env:JWT_EXPIRATION_SECONDS="3600"
.\mvnw.cmd spring-boot:run


## 9. Manual Postman Test Artifacts

Manual test artifacts are stored under: manual-tests/

detailed manual test documentation done by me are in: manual-tests/ReadMe.md


## 10. General Postman Setup

Before running a Postman collection, start from a clean database unless you intentionally want to keep previous data:

docker compose down -v
docker compose up -d
.\mvnw.cmd spring-boot:run


Then:
1. Open Postman
2. Import the desired collection from: manual-tests/postman/
3. Run the requests in order (Note that some of them might need some extra action which will be explained here later)
4. If the collection has multipart file-upload, select the required file from: manual-tests/files/

Most collections ue collection variables such as :

baseUrl
runId
adminToken
devToken
projectId
ticketId

The default base URL is:
http://localhost:8080



## 11. How to use Each Postman Collection

### 11.1 IssueFlow_Manual_Smoke_Tests.postman_collection.json

Purpose:
* Basic users/auth/projects/tickets smoke tests.
* Useful for checking that the main app is running correctly.

How to run:

1. Start PostgreSQL and the application.
2. Import the collection into Postman.
3. Run the collection in order.



### 11.2 IssueFlow_Comments_Mentions_Tests.postman_collection.json

Purpose:

* Comments API.
* @username mention parsing.
* Mention retrieval by user.
* Mention re-evaluation after comment update.

How to run:

1. Start PostgreSQL and the application.
2. Import the collection into Postman.
3. Run the collection in order.



### 11.3 IssueFlow_Ticket_Dependencies_Tests.postman_collection.json

Purpose:

* Ticket dependency creation.
* Dependency listing.
* Dependency removal.
* Duplicate/self/cross-project dependency validation.
* Preventing transition to DONE while blockers are unresolved.

How to run:

1. Start PostgreSQL and the application.
2. Import the collection into Postman.
3. Run the collection in order.


### 11.4 IssueFlow_Auto_Assignment_Workload_Tests.postman_collection.json

Purpose:

* Auto-assignment for tickets without assigneeId.
* Excluding ADMIN users from auto-assignment.
* Least-loaded DEVELOPER selection.
* Project workload endpoint.

How to run:

1. Start PostgreSQL and the application.
2. Import the collection into Postman.
3. Run the collection in order

### 11.5 IssueFlow_CSV_Import_Export_Tests.postman_collection.json

Purpose:

* CSV export.
* CSV import.
* Valid CSV import.
* Mixed CSV import with invalid rows.
* CSV escaping for commas and quotes.

How to run:

1. Start PostgreSQL and the application.
2. Import the collection into Postman.
3. Run the setup/export requests in order.
4. For import requests, manually select the needed CSV file from manual-tests/files/.

Required files:
manual-tests/files/issueflow-valid-import.csv
manual-tests/files/issueflow-mixed-import.csv

Special notes:

In Postman, open the multipart request body.
Set the file field to the correct CSV file.
projectId is sent as a multipart form field.
CSV import ignores the id column because database IDs are generated by the backend.





### 11.6 IssueFlow_Attachments_Tests.postman_collection.json


Purpose:

* Attachment upload.
* Attachment list.
* Attachment delete.
* Invalid content type rejection.
* 10 MB size limit validation.
* Missing ticket validation.

How to run:

1. Start PostgreSQL and the application.
2. Import the collection into Postman.
3. Run the collection in order.
4. For file upload requests, select the needed file from manual-tests/files/.

Required files:
manual-tests/files/issueflow-valid-attachment.txt
manual-tests/files/issueflow-invalid-attachment.html
manual-tests/files/issueflow-large-attachment-over-10mb.txt


Special notes:

issueflow-valid-attachment.txt should pass.
issueflow-invalid-attachment.html should fail because text/html is not allowed.
issueflow-large-attachment-over-10mb.txt should fail because it is larger than 10 MB.

Allowed attachment content types:

image/png
image/jpeg
application/pdf
text/plain


### 11.7 IssueFlow_Auto_Escalation_Tests.postman_collection.json

Purpose:

Background auto-escalation scheduler.
Overdue ticket priority escalation.
Ignoring DONE tickets.
Ignoring future due-date tickets.
isOverdue behavior for critical tickets.

How to run:

1. Start clean:

docker compose down -v
docker compose up -d
.\mvnw.cmd spring-boot:run

2. Import the collection into Postman
3. Run the folder: Setup- create users/project/tickets via Postman
4. IMPORTANT!!! wait at least 70 seconds after the setup finishes
    The scheduler has a 60-seconds initial delay, so waiting gievs it time to run
5. Run the folder: Verification- wait before running 
6. Make sure to wait another 70 seconds between the two requests there for optimal idempotency check before running the final request

Special notes:
Do not run setup and verification immediately back-to-back.
The scheduler is time-based, so manual waiting is required.
The Spring Boot console should show Hibernate queries against overdue tickets when the scheduler runs.



### 11.8 IssueFlow_Audit_Log_API_Tests.postman_collection.json
 
Purpose:

* Read-only Audit Log API.
* Audit log retrieval.
* Filtering by entityType, entityId, action, and actor.
* Invalid enum filter validation.
* JWT protection.

How to run:

1. Start PostgreSQL and the application.
2. Import the collection into Postman.
3. Run the collection in order.

Special notes:

No helper files are required.
The setup requests create project/ticket audit logs that are then queried.


### 11.9 IssueFlow_Final_Smoke_Regression.postman_collection.json

Purpose:

* Broad final smoke/regression validation across the completed backend.

This collection checks:

* authentication
* project creation
* ticket creation
* auto-assignment
* workload
* comments and mentions
* dependencies
* CSV export/import
* attachments
* audit logs
* basic security
* scheduler behavior

How to run:

1. Start clean:

docker compose down -v
docker compose up -d
.\mvnw.cmd spring-boot:run

2. Import the collection into Postman
3. Run the collection folder in order. (please do it manually in order to not miss details such as required file)
4. For CSV import request, select: manual-tests/files/issueflow-final-smoke-import.csv
5. For attachment upload request, select: manual-tests/files/issueflow-final-attachment.txt
6. For scheduler check: 
    Run request 26
    Wait at least 70 seconds
    Run request 27

Special notes:
This collection is a broad smoke test.
It does not replace the detailed stage-specific collections.
It is useful as one final manual sanity check before submission.




## 12 Troubleshooting

* java or javac is not recognized

Set JAVA_HOME and update Path in the current PowerShell session:

$env:JAVA_HOME="C:\Users\yhona\.jdks\ms-21.0.11"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version




* Port 8080 is already in use

Stop the existing process or change the server port.

On Windows, you can find the process using port 8080:
netstat -ano | findstr :8080

Then stop the process if needed


* Database contains old test data or Database has old enum/check-constraint values

Rest the database via the following commands
docker compose down -v
docker compose up -d

* Auto-escalation test fails in Postman

The scheduler is time-based.
Run the setup requests, wait at least 70 seconds, and only then run the verification requests.
Do not run the entire setup and verification flow immediately back-to-back


* Token stopped working

If you called: POST /auth/logout
the token was revoked, login again to get a new token


## 13. Additional Notes

The project validates using both:

* Automated Maven integration/regression tests
* manual Postman collections

The details manual validation documentation is available in: manual-tests/ReadMe.md  