
-- Create the Participants table
CREATE TABLE Participants (
                              pid SERIAL PRIMARY KEY,
                              first_name VARCHAR(50) NOT NULL,
                              last_name VARCHAR(50) NOT NULL,
                              age INT,
                              gender VARCHAR(15),
                              phone VARCHAR(15),
                              email VARCHAR(100) UNIQUE
);

-- Create the Lobby table
CREATE TABLE Experiments (
                       exp_id SERIAL PRIMARY KEY,
                       pid1 INT,
                       pid2 INT,
                       FOREIGN KEY (pid1) REFERENCES Participants(pid),
                       FOREIGN KEY (pid2) REFERENCES Participants(pid)
);

CREATE TABLE ExperimentsFeedback ( 
                       exp_id INT NOT NULL,
                       pid INT NOT NULL,
                       question VARCHAR(100) NOT NULL,
                       answer VARCHAR(300) NOT NULL,
                       PRIMARY KEY (exp_id, pid, question),
                       FOREIGN KEY (pid) REFERENCES Participants(pid),
                       FOREIGN KEY (exp_id) REFERENCES Experiments(exp_id)
);

-- Create the Sessions table
CREATE TABLE Sessions ( -- SESSIONS WILL BE INSERTED TOGETHER WITH LOBBY
                          session_id SERIAL PRIMARY KEY,
                          exp_id INT,
                          duration INT,
                          session_type VARCHAR(50),
                          session_order INT,
                          tolerance INT,
                          window_length INT,
                          state VARCHAR(50),
                          FOREIGN KEY (exp_id) REFERENCES Experiments(exp_id)
);

CREATE TABLE SessionsFeedback ( 
                       exp_id INT NOT NULL,
                       session_id INT NOT NULL,
                       pid INT NOT NULL,
                       question VARCHAR(100) NOT NULL,
                       answer VARCHAR(300) NOT NULL,
                       PRIMARY KEY (exp_id, session_id, pid, question),
                       FOREIGN KEY (pid) REFERENCES Participants(pid),
                       FOREIGN KEY (session_id) REFERENCES Sessions(session_id),
                       FOREIGN KEY (exp_id) REFERENCES Experiments(exp_id)
);


-- Create the SessionUserInputEvent table
CREATE TABLE SessionEvents (
                                        event_id SERIAL PRIMARY KEY,
                                        session_id INT,
                                        type VARCHAR(50) NOT NULL,
                                        subtype VARCHAR(50) NOT NULL,
                                        timestamp INT NOT NULL,
                                        actor VARCHAR(100) NOT NULL,
                                        data TEXT,
                                        FOREIGN KEY (session_id) REFERENCES Sessions(session_id)
);
