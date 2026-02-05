# Quezic YouTube Proxy

A self-hosted proxy server that enables reliable YouTube audio streaming for the Quezic app. Uses yt-dlp to extract audio streams, bypassing YouTube's anti-bot measures.

## Features

- Reliable YouTube audio extraction using yt-dlp
- Stream caching (30 minutes) for faster responses
- Proxy endpoint for streaming through the server
- Direct URL endpoint for downloads
- Health check endpoint for monitoring

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /` | Server info and version |
| `GET /health` | Health check (returns yt-dlp version) |
| `GET /stream/:videoId` | Get stream URL and metadata (JSON) |
| `GET /proxy/:videoId` | Stream audio directly through server |

## Deployment Options

### Option 1: Railway (Recommended - Free Tier)

1. Fork/push this repo to GitHub
2. Go to [railway.app](https://railway.app)
3. Click "New Project" → "Deploy from GitHub repo"
4. Select your repo and the `proxy-server` folder
5. Railway will auto-detect the Dockerfile and deploy
6. Copy your app URL (e.g., `https://quezic-proxy-production.up.railway.app`)

### Option 2: Render (Free Tier)

1. Push to GitHub
2. Go to [render.com](https://render.com)
3. Click "New" → "Web Service"
4. Connect your GitHub repo
5. Set:
   - **Root Directory**: `proxy-server`
   - **Runtime**: Docker
6. Click "Create Web Service"
7. Copy your app URL (e.g., `https://quezic-youtube-proxy.onrender.com`)

### Option 3: Fly.io (Free Tier)

1. Install Fly CLI: `brew install flyctl`
2. Login: `fly auth login`
3. Navigate to proxy-server folder:
   ```bash
   cd proxy-server
   ```
4. Launch (first time):
   ```bash
   fly launch --no-deploy
   # Edit fly.toml if needed, then:
   fly deploy
   ```
5. Get your URL: `fly status`

### Option 4: Local Docker (Current Setup)

```bash
cd proxy-server
docker compose up -d --build
```

Your proxy will be at `http://localhost:3000` or `http://YOUR_IP:3000`

## Configuration in Quezic App

1. Open Quezic app
2. Go to Settings (gear icon in Library)
3. Enter your proxy URL:
   - Local: `http://192.168.x.x:3000`
   - Cloud: `https://your-app.railway.app` (or render/fly URL)
4. Enable the proxy toggle
5. Tap "Test" to verify connection
6. Tap "Save"

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | 3000 | Server port |

## Updating yt-dlp

yt-dlp is updated frequently to keep up with YouTube changes. To update:

**Docker (rebuild)**:
```bash
docker compose down
docker compose up -d --build --no-cache
```

**Cloud**: Redeploy your app (Railway/Render/Fly will rebuild automatically)

## Troubleshooting

### "Video unavailable" errors
- The video may be geo-restricted, private, or age-gated
- Try a different video to verify the proxy works

### Slow first request
- yt-dlp needs to initialize on first request
- Subsequent requests use cache and are faster

### Cloud deployment issues
- Ensure your Dockerfile builds successfully locally first
- Check platform logs for errors
- Free tiers may have cold start delays (10-30s)

## Security Notes

- This proxy is designed for personal use
- Don't expose without authentication in production
- Consider adding rate limiting for public deployments

## License

Part of the Quezic project.
