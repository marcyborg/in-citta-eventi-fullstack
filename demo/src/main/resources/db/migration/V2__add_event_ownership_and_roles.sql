ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ALTER COLUMN password SET NOT NULL;
ALTER TABLE users ADD COLUMN role VARCHAR(16) DEFAULT 'USER' NOT NULL;
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));

-- Historical events retain a NULL owner, editable only by an administrator.
ALTER TABLE event ADD COLUMN owner_id BIGINT;
ALTER TABLE event ADD CONSTRAINT fk_event_owner FOREIGN KEY (owner_id) REFERENCES users(id);

CREATE INDEX ix_event_owner ON event(owner_id);
CREATE INDEX ix_event_data ON event(data);
CREATE INDEX ix_event_categoria_data ON event(categoria, data);
