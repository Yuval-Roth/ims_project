-- Define the enum types
CREATE TYPE gender_enum AS ENUM ('Male', 'Female');
CREATE TYPE session_type_enum AS ENUM (
    'user_input', 'sensor_data', 'network_data', 'sync_data', 'meta_data'
);
CREATE TYPE session_subtype_enum AS ENUM (
    -- USER_INPUT
    'click', 'angle', 'rotation',
    -- SENSOR_DATA
    'heart_rate', 'heart_rate_variability', 'blood_oxygen', 'gyroscope', 'accelerometer',
    -- NETWORK_DATA
    'latency', 'packet_out_of_order', 'timeout',
    -- SYNC_DATA
    'sync_start_time', 'sync_end_time', 'synced_at_time',
    -- META_DATA
    'server_start_time', 'client_start_time', 'time_server_delta', 'session_started', 'session_ended'
);

-- Create the Participants table
CREATE TABLE Participants (
                              pid SERIAL PRIMARY KEY,
                              first_name VARCHAR(50) NOT NULL,
                              last_name VARCHAR(50) NOT NULL,
                              age INT,
                              gender gender_enum,
                              phone VARCHAR(15),
                              email VARCHAR(100) UNIQUE
);

-- Create the Lobby table
CREATE TABLE Lobbies (
                       lobby_id SERIAL PRIMARY KEY,
                       pid1 INT,
                       pid2 INT,
                       FOREIGN KEY (pid1) REFERENCES Participants(pid) ON DELETE SET NULL,
                       FOREIGN KEY (pid2) REFERENCES Participants(pid) ON DELETE SET NULL
);

-- Create the Sessions table
CREATE TABLE Sessions (
                          session_id SERIAL PRIMARY KEY,
                          lobby_id INT,
                          duration INT,
                          session_type session_type_enum,
                          session_order INT,
                          FOREIGN KEY (lobby_id) REFERENCES Lobbies(lobby_id) ON DELETE CASCADE
);

-- Create the SessionUserInputEvent table
CREATE TABLE SessionEvents_user_input (
                                       event_id SERIAL PRIMARY KEY,
                                        session_id INT,
                                       type session_type_enum NOT NULL,
                                       subtype session_subtype_enum NOT NULL,
                                       timestamp BIGINT NOT NULL,
                                       actor VARCHAR(100) NOT NULL,
                                       data TEXT,
                                       FOREIGN KEY (session_id) REFERENCES Sessions(session_id) ON DELETE CASCADE
);
