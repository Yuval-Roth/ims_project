
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
CREATE TABLE Lobbies ( -- CHANGE NAME TO EXPERIMENT
                       lobby_id SERIAL PRIMARY KEY,
                       pid1 INT,
                       pid2 INT,
                       FOREIGN KEY (pid1) REFERENCES Participants(pid) ON DELETE SET NULL,
                       FOREIGN KEY (pid2) REFERENCES Participants(pid) ON DELETE SET NULL
);

-- Create the Sessions table
CREATE TABLE Sessions ( -- SESSIONS WILL BE INSERTED TOGETHER WITH LOBBY
                          session_id SERIAL PRIMARY KEY,
                          lobby_id INT,
                          duration INT,
                          session_type VARCHAR(50),
                          session_order INT,
                          tolerance INT
                          window_length INT
                          FOREIGN KEY (lobby_id) REFERENCES Lobbies(lobby_id) ON DELETE CASCADE
);

-- Create the SessionUserInputEvent table
CREATE TABLE SessionEvents ( -- AT THE END OF EVERY SESSION, A LIST OF EVENTS WILL BE PASSED
                                       event_id SERIAL PRIMARY KEY,
                                        session_id INT,
                                       type VARCHAR(50) NOT NULL,
                                       subtype VARCHAR(50) NOT NULL,
                                       timestamp BIGINT NOT NULL, --ms from start of session, can be INT
                                       actor VARCHAR(100) NOT NULL,
                                       data TEXT,
                                       FOREIGN KEY (session_id) REFERENCES Sessions(session_id) ON DELETE CASCADE
);
