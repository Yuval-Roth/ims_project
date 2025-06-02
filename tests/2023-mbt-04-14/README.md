# Software Quality Engineering - System Testing
This is a repository for the system-testing assignment of the Software Quality Engineering course at the [Ben-Gurion University](https://in.bgu.ac.il/), Israel.

## Assignment Description
In this assignment, we tested an open-source software called Moodle (https://download.moodle.org/windows/).

$$*TODO* Add some general description about the software$$

## Installation
From Moodle's documentation page: 
Moodle is a free, online Learning Management system enabling educators to create their own 
private website filled with dynamic courses that extend learning, any time, anywhere.

Provengo Moodle Setup instructions:
*make sure to choose English in Moodle's installation, choosing hebrew may because a bug for some
1. Login into the admin user.
2. Create two users:
   A. First Name: Bob
      Last Name: Jones
      Username: bob
      Password: Bob1234!
      Email: bob.jones@bobJones.com
   B. First Name: Charlie
      Last Name: Kirk
      Username: charlie
      Password: Charlie1234!
      Email: charlie.kirk@charliekirk.com
Leave all other non-mandatory fields with their empty/default values.
3. Create a new course:
   Full Name: Course
   Short Name: Course
   Category: Category 1 (existing default category)
4. In the participants tab in the course page, enroll Bob as a lecturer and Charlie as a student.
5. Log out of the admin user.
6. Login with bob user.
7. Go into "course"
8. Create new assignment :, with maximum files set to 2.
   Name : Assignment
   Maximum number of files : 2
9. Log out from bob.

## What we tested

We tested the submission module, created and fine-tuned by a teacher to allow a student to submit files, 
restrained by condition defined by the teacher such as the number and type of files,
the window in which file submission is allowed, etc.

*User story:* A teacher opens an assignment with a file submission limit of two, 
a student attempts to submit two files, before they save the teacher changes the file submission limit to one.

*Preconditions:* There is a course with a teacher and an open assignment, and at least one student enrolled in it.

*Expected outcome:* The student receives an error message, and is request to attempt submission again, or remove a file


*User story:* A teacher opens an assignment with a file submission limit of two,
a student submits two files, and then the teacher changes the file submission limit to one.

*Preconditions:* There is a course with a teacher and an open assignment, and at least one student enrolled in it.

*Expected outcome:* The student is notified that his submission is not longer valid, and is requested to amend it.

## How we tested
We used two different testing methods:
1. [Cucumber](https://cucumber.io/), a behavior-driven testing framework.
2. [Provengo](https://provengo.tech/), a story-based testing framework.

Each of the testing methods is elaborated in its own directory. 

## Results
Update all README.md files (except for d-e, see Section 1). Specifically, replace all $$*TODO*…$$ according to the instructions inside the $$.

## Detected Bugs
We detected the following bugs:

1. Bug 1: 
   1. General description: ...
   2. Steps to reproduce: ...
   3. Expected result: ...
   4. Actual result: ...
   5. Link to the bug report: (you are encouraged to report the bug to the developers of the software)
2. Bug 2: ...

$$*TODO* if you did not detect the bug, you should delete this section$$  
