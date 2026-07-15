-- Create all microservice databases if they don't exist
CREATE DATABASE auth_db;
CREATE DATABASE user_db;
CREATE DATABASE content_db;
CREATE DATABASE notification_db;

-- Grant permissions to your user profile
GRANT ALL PRIVILEGES ON DATABASE auth_db TO pradeep;
GRANT ALL PRIVILEGES ON DATABASE user_db TO pradeep;
GRANT ALL PRIVILEGES ON DATABASE content_db TO pradeep;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO pradeep;