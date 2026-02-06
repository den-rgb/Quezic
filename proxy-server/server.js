/**
 * Quezic YouTube Proxy Server
 * 
 * Uses yt-dlp for reliable YouTube audio extraction.
 * Supports cookie authentication to bypass bot detection.
 */

const express = require('express');
const { spawn, exec } = require('child_process');
const NodeCache = require('node-cache');
const cors = require('cors');
const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const { promisify } = require('util');

const execAsync = promisify(exec);

const app = express();
const PORT = process.env.PORT || 3000;

// Cookie file path
const COOKIE_FILE = process.env.COOKIE_FILE || path.join(__dirname, 'cookies.txt');

// Cache stream URLs for 30 minutes (YouTube URLs expire after ~6 hours)
const streamCache = new NodeCache({ stdTTL: 1800, checkperiod: 300 });

// Concurrency limiter to prevent memory exhaustion
const MAX_CONCURRENT_EXTRACTIONS = 2; // Limit to 2 simultaneous yt-dlp processes
let currentExtractions = 0;
const extractionQueue = [];

/**
 * Queue wrapper for extraction to limit memory usage
 */
function queueExtraction(videoId) {
    return new Promise((resolve, reject) => {
        const task = { videoId, resolve, reject };
        
        if (currentExtractions < MAX_CONCURRENT_EXTRACTIONS) {
            runExtraction(task);
        } else {
            console.log(`Queuing extraction for ${videoId} (${extractionQueue.length + 1} in queue)`);
            extractionQueue.push(task);
        }
    });
}

async function runExtraction(task) {
    currentExtractions++;
    try {
        const result = await doExtractStreamUrl(task.videoId);
        task.resolve(result);
    } catch (error) {
        task.reject(error);
    } finally {
        currentExtractions--;
        // Process next in queue
        if (extractionQueue.length > 0) {
            const next = extractionQueue.shift();
            runExtraction(next);
        }
    }
}

/**
 * Check if cookies file exists and is valid
 */
function hasCookies() {
    try {
        if (fs.existsSync(COOKIE_FILE)) {
            const content = fs.readFileSync(COOKIE_FILE, 'utf8');
            // Check for Netscape cookie format - needs YouTube or Google auth cookies
            return content.includes('.youtube.com') || content.includes('youtube.com') || 
                   content.includes('.google.com');
        }
    } catch (e) {
        console.log('Cookie file check error:', e.message);
    }
    return false;
}

app.use(cors());
app.use(express.json());

/**
 * Extract audio stream URL using yt-dlp (queued to limit memory usage)
 */
async function extractStreamUrl(videoId) {
    // Check cache first
    const cached = streamCache.get(videoId);
    if (cached) {
        console.log(`Cache hit for ${videoId}`);
        return cached;
    }
    
    // Use queue to limit concurrent extractions
    return queueExtraction(videoId);
}

/**
 * Actual extraction logic - called from queue
 * Uses cookies if available, then tries multiple client strategies
 */
async function doExtractStreamUrl(videoId) {
    // Double-check cache (might have been populated while waiting in queue)
    const cached = streamCache.get(videoId);
    if (cached) {
        console.log(`Cache hit for ${videoId} (after queue)`);
        return cached;
    }

    console.log(`Extracting stream for ${videoId} using yt-dlp...`);
    
    const url = `https://www.youtube.com/watch?v=${videoId}`;
    const cookiesAvailable = hasCookies();
    const cookieArg = cookiesAvailable ? `--cookies "${COOKIE_FILE}"` : '';
    
    if (cookiesAvailable) {
        console.log('Using cookies for authentication');
    } else {
        console.log('No cookies available, trying without authentication');
    }
    
    // Common args for age bypass and better extraction
    const ageBypass = '--age-limit 21';
    
    // Reduced strategies to save memory - only try the most effective ones
    const strategies = cookiesAvailable ? [
        // With cookies: web client usually works best
        {
            name: 'web_with_cookies',
            args: `${cookieArg} ${ageBypass} -f "bestaudio[ext=m4a]/bestaudio" -j`
        },
        // iOS as backup (good for age-restricted)
        {
            name: 'ios_with_cookies',
            args: `${cookieArg} ${ageBypass} --extractor-args "youtube:player_client=ios" -f "bestaudio[ext=m4a]/bestaudio" -j`
        }
    ] : [
        // Without cookies: try iOS first (least likely to be blocked)
        {
            name: 'ios_client',
            args: `${ageBypass} --extractor-args "youtube:player_client=ios" -f "bestaudio[ext=m4a]/bestaudio" -j`
        },
        {
            name: 'web_client',
            args: `${ageBypass} -f "bestaudio[ext=m4a]/bestaudio" -j`
        }
    ];
    
    let lastError = null;
    
    for (const strategy of strategies) {
        try {
            console.log(`Trying ${strategy.name} strategy...`);
            
            const { stdout, stderr } = await execAsync(
                `yt-dlp ${strategy.args} "${url}"`,
                { timeout: 60000 }
            );
            
            if (stderr && !stderr.includes('WARNING')) {
                console.log(`yt-dlp stderr (${strategy.name}):`, stderr);
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
            
            console.log(`Success with ${strategy.name}: ${mimeType}, ${result.bitrate}kbps`);
            
            streamCache.set(videoId, result);
            return result;
            
        } catch (error) {
            console.log(`${strategy.name} failed: ${error.message.substring(0, 150)}`);
            lastError = error;
            
            // Only break on truly unrecoverable errors
            if (error.message.includes('Video unavailable') || 
                error.message.includes('Private video') ||
                error.message.includes('removed') ||
                error.message.includes('terminated')) {
                break;
            }
            // For age verification, keep trying other strategies - some clients bypass it
        }
    }
    
    // All strategies failed
    const errorMsg = lastError?.message || 'Unknown error';
    
    if (errorMsg.includes('Video unavailable') || errorMsg.includes('Private video')) {
        throw new Error('Video is unavailable or private');
    }
    
    if (errorMsg.includes('age') || errorMsg.includes('confirm your age')) {
        throw new Error('Age-restricted video - try exporting cookies from non-private browser');
    }
    
    if (errorMsg.includes('Sign in') || errorMsg.includes('bot')) {
        throw new Error('YouTube requires authentication - upload fresh cookies');
    }
    
    if (errorMsg.includes('copyright') || errorMsg.includes('blocked')) {
        throw new Error('Video blocked in proxy region');
    }
    
    throw new Error(`Extraction failed: ${errorMsg.substring(0, 200)}`);
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
 * Follow redirects and make request to final URL
 * @param {string} url - URL to fetch
 * @param {object} headers - Request headers
 * @param {number} maxRedirects - Maximum redirects to follow
 * @returns {Promise<{response: IncomingMessage, finalUrl: string}>}
 */
function fetchWithRedirects(url, headers, maxRedirects = 5) {
    return new Promise((resolve, reject) => {
        const makeRequest = (currentUrl, redirectCount) => {
            if (redirectCount > maxRedirects) {
                reject(new Error('Too many redirects'));
                return;
            }
            
            const protocol = currentUrl.startsWith('https') ? https : http;
            const options = { 
                headers,
                timeout: 600000  // 10 minute timeout for large files
            };
            
            const req = protocol.get(currentUrl, options, (response) => {
                // Handle redirects (301, 302, 303, 307, 308)
                if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
                    const redirectUrl = new URL(response.headers.location, currentUrl).toString();
                    console.log(`Following redirect to: ${redirectUrl.substring(0, 100)}...`);
                    response.destroy(); // Clean up the response
                    makeRequest(redirectUrl, redirectCount + 1);
                    return;
                }
                
                resolve({ response, finalUrl: currentUrl });
            });
            
            req.setTimeout(600000); // 10 minute timeout
            req.on('timeout', () => {
                req.destroy();
                reject(new Error('Request timeout'));
            });
            req.on('error', reject);
        };
        
        makeRequest(url, 0);
    });
}

/**
 * GET /proxy/:videoId - Proxy the actual audio stream
 * Streams audio through the server with proper headers
 */
app.get('/proxy/:videoId', async (req, res) => {
    const { videoId } = req.params;

    // Disable timeout for this request (large file downloads)
    req.setTimeout(0);
    res.setTimeout(0);

    if (!videoId || !/^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
        return res.status(400).json({ error: 'Invalid video ID' });
    }

    try {
        const stream = await extractStreamUrl(videoId);
        const audioUrl = stream.url;
        
        console.log(`Proxying audio for ${videoId}...`);
        
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
        
        // Fetch with redirect following
        const { response: proxyRes, finalUrl } = await fetchWithRedirects(audioUrl, requestHeaders);
        
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
        
        // Handle client disconnect
        req.on('close', () => {
            proxyRes.destroy();
        });
        
        proxyRes.on('error', (err) => {
            console.error(`Proxy response error: ${err.message}`);
            if (!res.headersSent) {
                res.status(500).json({ error: 'Failed to proxy stream' });
            }
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
 * POST /cookies - Upload YouTube cookies
 * Accepts Netscape format cookies (from browser extension or yt-dlp --cookies-from-browser)
 */
app.post('/cookies', express.text({ type: '*/*', limit: '1mb' }), (req, res) => {
    try {
        const cookies = req.body;
        
        if (!cookies || typeof cookies !== 'string') {
            return res.status(400).json({ error: 'Cookie data required in request body' });
        }
        
        // Validate it looks like Netscape cookie format with YouTube or Google auth cookies
        const hasYouTube = cookies.includes('.youtube.com') || cookies.includes('youtube.com');
        const hasGoogle = cookies.includes('.google.com');
        if (!hasYouTube && !hasGoogle) {
            return res.status(400).json({ error: 'Invalid cookie format - must contain YouTube or Google cookies' });
        }
        
        // Write cookies to file
        fs.writeFileSync(COOKIE_FILE, cookies, 'utf8');
        
        // Clear cache to force re-extraction with new cookies
        streamCache.flushAll();
        
        const cookieLines = cookies.split('\n').filter(l => l.trim() && !l.startsWith('#'));
        console.log(`Cookies updated successfully (${cookieLines.length} entries)`);
        res.json({ 
            success: true, 
            message: `Cookies saved successfully (${cookieLines.length} entries)`,
            cookieCount: cookieLines.length
        });
    } catch (error) {
        console.error('Cookie save error:', error);
        res.status(500).json({ error: 'Failed to save cookies: ' + error.message });
    }
});

/**
 * DELETE /cookies - Remove cookies
 */
app.delete('/cookies', (req, res) => {
    try {
        if (fs.existsSync(COOKIE_FILE)) {
            fs.unlinkSync(COOKIE_FILE);
            streamCache.flushAll();
            console.log('Cookies deleted');
        }
        res.json({ success: true, message: 'Cookies removed' });
    } catch (error) {
        res.status(500).json({ error: 'Failed to delete cookies: ' + error.message });
    }
});

/**
 * GET /cookies/status - Check if cookies are configured
 */
app.get('/cookies/status', (req, res) => {
    const hasValidCookies = hasCookies();
    res.json({
        hasCookies: hasValidCookies,
        cookieFile: hasValidCookies ? 'configured' : 'not found'
    });
});

/**
 * GET /health - Health check
 */
app.get('/health', async (req, res) => {
    // Check if yt-dlp is available
    let ytdlpVersion = 'unknown';
    let denoVersion = 'unknown';
    
    try {
        const { stdout } = await execAsync('yt-dlp --version', { timeout: 5000 });
        ytdlpVersion = stdout.trim();
    } catch (e) {
        ytdlpVersion = 'not installed';
    }
    
    try {
        const { stdout } = await execAsync('deno --version', { timeout: 5000 });
        denoVersion = stdout.split('\n')[0].trim();
    } catch (e) {
        denoVersion = 'not installed';
    }
    
    // Memory usage
    const memUsage = process.memoryUsage();
    const memMB = Math.round(memUsage.heapUsed / 1024 / 1024);
    
    res.json({ 
        status: 'ok',
        cacheSize: streamCache.keys().length,
        ytdlpVersion,
        denoVersion,
        hasCookies: hasCookies(),
        queue: {
            active: currentExtractions,
            waiting: extractionQueue.length,
            maxConcurrent: MAX_CONCURRENT_EXTRACTIONS
        },
        memoryMB: memMB
    });
});

/**
 * GET / - Info
 */
app.get('/', (req, res) => {
    res.json({
        name: 'Quezic YouTube Proxy',
        version: '2.1.0',
        engine: 'yt-dlp',
        hasCookies: hasCookies(),
        endpoints: {
            '/stream/:videoId': 'Get audio stream URL and info (JSON)',
            '/proxy/:videoId': 'Stream audio directly through proxy',
            '/health': 'Health check',
            '/cookies/status': 'Check cookie status',
            'POST /cookies': 'Upload YouTube cookies (Netscape format)',
            'DELETE /cookies': 'Remove cookies'
        },
        usage: 'For best results, upload YouTube cookies via POST /cookies',
        cookieHelp: 'Export cookies using browser extension or: yt-dlp --cookies-from-browser chrome --cookies cookies.txt'
    });
});

// Start server with extended timeouts for large file downloads
const server = app.listen(PORT, () => {
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

// Set server timeouts for long-running downloads
server.keepAliveTimeout = 620000;  // 10+ minutes
server.headersTimeout = 630000;    // Slightly longer than keepAlive
server.timeout = 0;                // No timeout for the server itself
