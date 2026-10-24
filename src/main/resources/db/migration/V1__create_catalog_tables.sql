CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL,
    category_id UUID NOT NULL,
    active BOOLEAN NOT NULL,
    stock_quantity INTEGER NOT NULL,
    created_at TIMESTAMP
    WITH
        TIME ZONE NOT NULL,
        updated_at TIMESTAMP
    WITH
        TIME ZONE NOT NULL,
        CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);