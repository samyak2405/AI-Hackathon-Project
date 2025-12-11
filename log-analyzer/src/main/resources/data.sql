-- Sample data for testing
-- Insert sample transactions (will fail silently if duplicates exist due to unique constraint)
INSERT INTO transactions (transaction_id, uuid, service_id) 
SELECT 'abc123-xyz', '550e8400-e29b-41d4-a716-446655440000', 'SERVICE-001'
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE transaction_id = 'abc123-xyz');

INSERT INTO transactions (transaction_id, uuid, service_id) 
SELECT 'def456-uvw', '6ba7b810-9dad-11d1-80b4-00c04fd430c8', 'SERVICE-002'
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE transaction_id = 'def456-uvw');

