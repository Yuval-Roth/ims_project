CREATE TABLE IF NOT EXISTS Participants (
    pid SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(15) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS Experiments (
    exp_id SERIAL PRIMARY KEY,
    pid1 INT,
    pid2 INT,
    FOREIGN KEY (pid1) REFERENCES Participants(pid),
    FOREIGN KEY (pid2) REFERENCES Participants(pid)
);

CREATE TABLE IF NOT EXISTS ExperimentsFeedback ( 
    exp_id INT,
    pid INT,
    question VARCHAR(100),
    answer VARCHAR(300) NOT NULL,
    PRIMARY KEY (exp_id, pid, question),
    FOREIGN KEY (pid) REFERENCES Participants(pid),
    FOREIGN KEY (exp_id) REFERENCES Experiments(exp_id)
);

CREATE TABLE IF NOT EXISTS Sessions (
    session_id SERIAL PRIMARY KEY,
    exp_id INT,
    duration INT NOT NULL,
    session_type VARCHAR(50) NOT NULL,
    session_order INT NOT NULL,
    tolerance INT NOT NULL,
    window_length INT NOT NULL,
    state VARCHAR(50) NOT NULL,
    FOREIGN KEY (exp_id) REFERENCES Experiments(exp_id)
);

CREATE TABLE IF NOT EXISTS SessionsFeedback ( 
    session_id INT,
    pid INT,
    question VARCHAR(100),
    answer VARCHAR(300) NOT NULL,
    PRIMARY KEY (session_id, pid, question),
    FOREIGN KEY (pid) REFERENCES Participants(pid),
    FOREIGN KEY (session_id) REFERENCES Sessions(session_id)
);

CREATE TABLE IF NOT EXISTS SessionEvents (
    event_id SERIAL PRIMARY KEY,
    session_id INT,
    type VARCHAR(50) NOT NULL,
    subtype VARCHAR(50) NOT NULL,
    timestamp INT NOT NULL,
    actor VARCHAR(100) NOT NULL,
    data TEXT,
    FOREIGN KEY (session_id) REFERENCES Sessions(session_id)
);

CREATE TABLE IF NOT EXISTS Credentials (
    user_id VARCHAR(50) PRIMARY KEY,
    password VARCHAR(50) NOT NULL
)
