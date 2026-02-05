/**
 * Quezic YouTube Proxy Server
 * 
 * Uses yt-dlp for reliable YouTube audio extraction.
 * Falls back to Puppeteer for videos that require browser context.
 */

const express = require('express');
const { spawn, exec } = require('child_process');
const NodeCache = require('node-cache');
const cors = require('cors');
const http = require('http');
const https = require('https');
const { promisify } = require('util');

const execAsync = promisify(exec);

const app = express();
const PORT = process.env.PORT || 3000;

// Cache stream URLs for 30 minutes (YouTube URLs expire after ~6 hours)
const streamCache = new NodeCache({ stdTTL: 1800, checkperiod: 300 });

app.use(cors());
app.use(express.json());

/**
 * Extract audio stream URL using yt-dlp
 */
async function extractStreamUrl(videoId) {
    // Check cache first
    const cached = streamCache.get(videoId);
    if (cached) {
        console.log(`Cache hit for ${videoId}`);
        return cached;
    }

    console.log(`Extracting stream for ${videoId} using yt-dlp...`);
    
    const url = `https://www.youtube.com/watch?v=${videoId}`;
    
    try {
        // Use yt-dlp to get the best audio stream
        // -f: format selection (best audio only)
        // -g: get URL only
        // -j: output JSON with full info
        const { stdout, stderr } = await execAsync(
            `yt-dlp -f "bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio" -j "${url}"`,
            { timeout: 60000 }
        );
        
        if (stderr) {
            console.log('yt-dlp stderr:', stderr);
        }
        
        const info = JSON.parse(stdout);
        
        // Extract the audio URL
        const audioUrl = info.url;
        if (!audioUrl) {
            throw new Error('No audio URL in yt-dlp output');
        }
        
        // Get format info
        const mimeType = info.ext === 'webm' ? 'audio/webm' : 
                         info.ext === 'm4a' ? 'audio/mp4' :
                         info.ext === 'opus' ? 'audio/opus' :
                         'audio/mp4';
        
        const result = {
            url: audioUrl,
            mimeType,
            videoId,
            title: info.title || '',
            channel: info.uploader || info.channel || '',
            duration: info.duration || 0,
            bitrate: info.abr || info.tbr || 0,
            format: info.format || ''
        };
        
        console.log(`Successfully extracted: ${mimeType}, ${result.bitrate}kbps, format: ${info.format_id}`);
        
        streamCache.set(videoId, result);
        return result;
        
    } catch (error) {
        console.error(`yt-dlp extraction failed: ${error.message}`);
        
        // Check if it's a specific error we can handle
        if (error.message.includes('Video unavailable') || 
            error.message.includes('Private video')) {
            throw new Error('Video is unavailable or private');
        }
        
        if (error.message.includes('age')) {
            throw new Error('Video requires age verification');
        }
        
        throw error;
    }
}

/**
 * GET /stream/:videoId - Get stream URL info
 */
app.get('/stream/:videoId', async (req, res) => {
    const { videoId } = req.params;

    if (!videoId || !/^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
        return res.status(400).json({ error: 'Invalid video ID' });
    }

    try {
        const stream = await extractStreamUrl(videoId);
        res.json(stream);
    } catch (error) {
        console.error(`Error for ${videoId}:`, error.message);
        res.status(500).json({ 
            error: error.message,
            videoId 
        });
    }
});

/**
 * HEAD /proxy/:videoId - Get stream info without downloading
 * Used by media players to probe the stream
 */
app.head('/proxy/:videoId', async (req, res) => {
    const { videoId } = req.params;

    if (!videoId || !/^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
        return res.status(400).end();
    }

    try {
        const stream = await extractStreamUrl(videoId);
        
        res.setHeader('Content-Type', stream.mimeType || 'audio/mp4');
        res.setHeader('Accept-Ranges', 'bytes');
        res.setHeader('Access-Control-Allow-Origin', '*');
        
        // Extract content length from URL if available
        const clenMatch = stream.url.match(/clen=(\d+)/);
        if (clenMatch) {
            res.setHeader('Content-Length', clenMatch[1]);
        }
        
        res.status(200).end();
    } catch (error) {
        res.status(500).end();
    }
});

/**
 * GET /proxy/:videoId - Proxy the actual audio stream
 * Streams audio through the server with proper headers
 */
app.get('/proxy/:videoId', async (req, res) => {
    const { videoId } = req.params;

    if (!videoId || !/^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
        return res.status(400).json({ error: 'Invalid video ID' });
    }

    try {
        const stream = await extractStreamUrl(videoId);
        const audioUrl = stream.url;
        
        console.log(`Proxying audio for ${videoId}...`);
        
        // Fetch and stream the audio
        const protocol = audioUrl.startsWith('https') ? https : http;
        
        // Handle range requests for seeking
        const range = req.headers.range;
        const requestHeaders = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Accept': '*/*',
            'Accept-Language': 'en-US,en;q=0.9',
            'Referer': 'https://www.youtube.com/',
            'Origin': 'https://www.youtube.com'
        };
        
        if (range) {
            requestHeaders['Range'] = range;
        }
        
        const proxyReq = protocol.get(audioUrl, { headers: requestHeaders }, (proxyRes) => {
            // Get content type from response or use our extracted mime type
            let contentType = proxyRes.headers['content-type'];
            if (!contentType || contentType === 'application/octet-stream' || contentType.includes('vnd.yt')) {
                contentType = stream.mimeType || 'audio/mp4';
            }
            
            // Forward status code
            res.status(proxyRes.statusCode);
            
            // Set headers
            res.setHeader('Content-Type', contentType);
            
            if (proxyRes.headers['content-length']) {
                res.setHeader('Content-Length', proxyRes.headers['content-length']);
            }
            if (proxyRes.headers['content-range']) {
                res.setHeader('Content-Range', proxyRes.headers['content-range']);
            }
            
            res.setHeader('Accept-Ranges', 'bytes');
            res.setHeader('Access-Control-Allow-Origin', '*');
            res.setHeader('Cache-Control', 'no-cache');
            
            console.log(`Streaming ${contentType}, status: ${proxyRes.statusCode}, size: ${proxyRes.headers['content-length'] || 'unknown'}`);
            
            // Pipe the response
            proxyRes.pipe(res);
        });
        
        proxyReq.on('error', (err) => {
            console.error(`Proxy request error: ${err.message}`);
            if (!res.headersSent) {
                res.status(500).json({ error: 'Failed to proxy stream' });
            }
        });
        
        // Handle client disconnect
        req.on('close', () => {
            proxyReq.destroy();
        });
        
    } catch (error) {
        console.error(`Proxy error for ${videoId}:`, error.message);
        res.status(500).json({ 
            error: error.message,
            videoId 
        });
    }
});

/**
 * GET /health - Health check
 */
app.get('/health', async (req, res) => {
    // Check if yt-dlp is available
    let ytdlpVersion = 'unknown';
    try {
        const { stdout } = await execAsync('yt-dlp --version', { timeout: 5000 });
        ytdlpVersion = stdout.trim();
    } catch (e) {
        ytdlpVersion = 'not installed';
    }
    
    res.json({ 
        status: 'ok',
        cacheSize: streamCache.keys().length,
        ytdlpVersion
    });
});

/**
 * GET / - Info
 */
app.get('/', (req, res) => {
    res.json({
        name: 'Quezic YouTube Proxy',
        version: '2.0.0',
        engine: 'yt-dlp',
        endpoints: {
            '/stream/:videoId': 'Get audio stream URL and info (JSON)',
            '/proxy/:videoId': 'Stream audio directly through proxy',
            '/health': 'Health check'
        },
        usage: 'For best compatibility, use /proxy/:videoId to stream audio directly'
    });
});

// Start server
app.listen(PORT, () => {
    console.log(`Quezic YouTube Proxy v2 running on port ${PORT}`);
    console.log('Using yt-dlp for reliable YouTube extraction');
    
    // Check yt-dlp version on startup
    exec('yt-dlp --version', (error, stdout, stderr) => {
        if (error) {
            console.error('WARNING: yt-dlp not found! Install with: pip install yt-dlp');
        } else {
            console.log(`yt-dlp version: ${stdout.trim()}`);
        }
    });
});
