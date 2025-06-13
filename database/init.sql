CREATE TABLE IF NOT EXISTS Participants (
    pid SERIAL PRIMARY KEY,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    age INT NOT NULL,
    gender TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS Experiments (
    exp_id SERIAL PRIMARY KEY,
    pid1 INT,
    pid2 INT,
    date_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pid1) REFERENCES Participants(pid),
    FOREIGN KEY (pid2) REFERENCES Participants(pid)
);

CREATE TABLE IF NOT EXISTS ExperimentsFeedback ( 
    exp_id INT,
    pid INT,
    question TEXT,
    answer TEXT NOT NULL,
    PRIMARY KEY (exp_id, pid, question),
    FOREIGN KEY (pid) REFERENCES Participants(pid),
    FOREIGN KEY (exp_id) REFERENCES Experiments(exp_id)
);

CREATE TABLE IF NOT EXISTS Sessions (
    session_id SERIAL PRIMARY KEY,
    exp_id INT,
    duration INT NOT NULL,
    session_type TEXT NOT NULL,
    session_order INT NOT NULL,
    tolerance INT NOT NULL,
    window_length INT NOT NULL,
    state TEXT NOT NULL,
    FOREIGN KEY (exp_id) REFERENCES Experiments(exp_id)
);

CREATE TABLE IF NOT EXISTS SessionsFeedback ( 
    session_id INT,
    pid INT,
    question TEXT,
    answer TEXT NOT NULL,
    PRIMARY KEY (session_id, pid, question),
    FOREIGN KEY (pid) REFERENCES Participants(pid),
    FOREIGN KEY (session_id) REFERENCES Sessions(session_id)
);

CREATE TABLE IF NOT EXISTS SessionEvents (
    event_id SERIAL PRIMARY KEY,
    session_id INT,
    type TEXT NOT NULL,
    subtype TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    actor TEXT NOT NULL,
    data TEXT,
    FOREIGN KEY (session_id) REFERENCES Sessions(session_id)
);

CREATE TABLE IF NOT EXISTS Credentials (
    user_id TEXT PRIMARY KEY,
    password TEXT NOT NULL
)
