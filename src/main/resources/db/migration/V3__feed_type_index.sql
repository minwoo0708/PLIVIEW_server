-- V3: 타입별 최신 피드(GET /api/feed?type=...)용 인덱스
CREATE INDEX idx_reviews_type_created ON reviews (type, created_at);
