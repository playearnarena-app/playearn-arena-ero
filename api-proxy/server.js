import https from "node:https";
import http from "node:http";

const PORT = Number(process.env.PORT || 10000);
const UPSTREAM_HOST = process.env.UPSTREAM_HOST || "130.210.13.115";
const ALLOWED_ORIGINS = new Set([
  "null",
  "https://playearn-arena-download.onrender.com"
]);

const server = http.createServer((req, res) => {
  const origin = req.headers.origin;
  if (origin && !ALLOWED_ORIGINS.has(origin)) {
    res.writeHead(403, { "Content-Type": "application/json", "Cache-Control": "no-store" });
    return res.end(JSON.stringify({ error: "Origin not allowed" }));
  }

  if (origin) {
    res.setHeader("Access-Control-Allow-Origin", origin);
    res.setHeader("Access-Control-Allow-Credentials", "true");
    res.setHeader("Vary", "Origin");
  }
  res.setHeader("Cache-Control", "no-store");

  if (req.method === "OPTIONS") {
    res.writeHead(204, {
      "Access-Control-Allow-Methods": "GET, POST, PUT, PATCH, DELETE, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Idempotency-Key, Authorization",
      "Access-Control-Max-Age": "600"
    });
    return res.end();
  }

  if (!(req.url.startsWith("/api/v1/") || req.url === "/healthz")) {
    res.writeHead(404, { "Content-Type": "application/json" });
    return res.end(JSON.stringify({ error: "Not found" }));
  }

  const headers = { ...req.headers, host: UPSTREAM_HOST };
  delete headers["connection"];
  delete headers["content-length"];
  const upstream = https.request({
    hostname: UPSTREAM_HOST,
    port: 443,
    path: req.url,
    method: req.method,
    headers,
    timeout: 15000
  }, upstreamRes => {
    const outHeaders = { ...upstreamRes.headers };
    delete outHeaders["connection"];
    delete outHeaders["transfer-encoding"];
    res.writeHead(upstreamRes.statusCode || 502, outHeaders);
    upstreamRes.pipe(res);
  });

  upstream.on("timeout", () => upstream.destroy(new Error("Upstream timeout")));
  upstream.on("error", err => {
    if (!res.headersSent) {
      res.writeHead(502, { "Content-Type": "application/json", "Cache-Control": "no-store" });
      res.end(JSON.stringify({ error: "API upstream unavailable" }));
    } else {
      res.destroy(err);
    }
  });
  req.pipe(upstream);
});

server.listen(PORT, "0.0.0.0", () => console.log("PlayEarn Arena API proxy listening on " + PORT));
