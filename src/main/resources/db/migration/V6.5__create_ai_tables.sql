-- 1. product_similarities
CREATE TABLE IF NOT EXISTS product_similarities (
    product_id         BIGINT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    similar_product_id BIGINT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    score              DOUBLE PRECISION NOT NULL,
    similarity_type    VARCHAR(50) DEFAULT 'CONTENT_BASED' NOT NULL,
    PRIMARY KEY (product_id, similar_product_id, similarity_type)
);

-- 2. user_recommendations
CREATE TABLE IF NOT EXISTS user_recommendations (
    user_id              BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    product_id           BIGINT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    recommendation_score DOUBLE PRECISION NOT NULL,
    created_at           TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, product_id)
);

-- 3. product_associations
CREATE TABLE IF NOT EXISTS product_associations (
    product_id         BIGINT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    related_product_id BIGINT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    confidence         DOUBLE PRECISION NOT NULL,
    lift               DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (product_id, related_product_id)
);
