# Build stage for GRPC and other extensions
FROM php:8.2-cli as builder

# Install system dependencies for building extensions
RUN apt-get update && apt-get install -y \
    git \
    unzip \
    libzip-dev \
    zlib1g-dev \
    autoconf \
    build-essential \
    libssl-dev \
    pkg-config

# Install and enable PHP extensions
RUN docker-php-ext-install zip sockets
RUN pecl install grpc && docker-php-ext-enable grpc

# Final stage
FROM php:8.2-cli

# Copy built extensions from builder
COPY --from=builder /usr/local/lib/php/extensions /usr/local/lib/php/extensions
COPY --from=builder /usr/local/etc/php/conf.d /usr/local/etc/php/conf.d

# Install only runtime dependencies
RUN apt-get update && apt-get install -y \
    git \
    unzip \
    libzip-dev \
    && rm -rf /var/lib/apt/lists/*

# Install Composer
COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

# Install RoadRunner CLI
RUN composer require spiral/roadrunner-cli --dev

# Download RoadRunner binary
RUN ./vendor/bin/rr get && chmod +x rr

# Set working directory
WORKDIR /app

# Copy composer files first to leverage Docker cache
COPY composer.json ./

# Install dependencies
ENV COMPOSER_ALLOW_SUPERUSER=1
RUN composer install --no-scripts --no-autoloader

# Copy application files
COPY . .

# Generate autoloader
RUN composer dump-autoload --optimize
