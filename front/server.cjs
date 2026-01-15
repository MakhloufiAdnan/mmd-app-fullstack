const express = require('express');
const path = require('path');
const compression = require('compression');
const { createProxyMiddleware } = require('http-proxy-middleware');

const PORT = process.env.PORT || 4300;
const API_TARGET = (process.env.API_TARGET ?? "").trim() || "http://localhost:8080";
console.log("API_TARGET =", API_TARGET);

// IMPORTANT: Angular build "public" files are in dist/<app>/browser
const DIST_DIR = path.join(process.cwd(), 'dist', 'front', 'browser');
const INDEX_HTML = path.join(DIST_DIR, 'index.html');

const app = express();
app.disable('x-powered-by');
app.use(compression());

// 1) Reverse proxy: /api -> backend
app.use(
  createProxyMiddleware({
    target: API_TARGET,
    changeOrigin: true,
    logLevel: "warn",
    pathFilter: "/api",
  })
);

// 2) Static files (Angular dist)
app.use(
  express.static(DIST_DIR, {
    index: false, // on gère l'index via le fallback SPA
    setHeaders(res, filePath) {
      // Cache "agressif" pour assets, pas pour HTML
      if (filePath.endsWith('.html')) {
        res.setHeader('Cache-Control', 'no-cache');
      } else {
        res.setHeader('Cache-Control', 'public, max-age=31536000, immutable');
      }
    },
  })
);

// 3) SPA fallback (routes Angular)
app.get(/^(?!\/api).*/, (req, res) => res.sendFile(INDEX_HTML));

app.listen(PORT, '0.0.0.0', () => {
  console.log(`✅ Front (static) + proxy (/api -> ${API_TARGET})`);
  console.log(`➡️  http://localhost:${PORT}`);
});
