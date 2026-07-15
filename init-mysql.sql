-- Create the university database schema
CREATE DATABASE IF NOT EXISTS university_db;

-- Grant global network permissions explicitly to bypass the 1044 Access Denied crash
GRANT ALL PRIVILEGES ON university_db.* TO 'pradeep'@'%';
FLUSH PRIVILEGES;