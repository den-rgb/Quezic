FROM node:18-slim

# Install Python, yt-dlp (latest), and Deno (required JS runtime for yt-dlp)
# ffmpeg helps with some edge cases
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    ffmpeg \
    curl \
    unzip \
    ca-certificates \
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Install Deno (required JS runtime for yt-dlp YouTube extraction)
RUN curl -fsSL https://deno.land/install.sh | DENO_INSTALL=/usr/local sh

# Install yt-dlp from GitHub releases (always latest)
RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp \
    && chmod a+rx /usr/local/bin/yt-dlp

# Verify installations
RUN deno --version && yt-dlp --version

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install Node.js dependencies
RUN npm install --omit=dev

# Copy source
COPY . .

# Expose port
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s \
    CMD node -e "require('http').get('http://localhost:3000/health', (r) => process.exit(r.statusCode === 200 ? 0 : 1))"

# Run the server
CMD ["npm", "start"]
