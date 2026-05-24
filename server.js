const http = require("http");
const fs = require("fs");
const path = require("path");

const root = __dirname;
const basePort = Number(process.env.PORT || 4173);
const mimeTypes = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".webmanifest": "application/manifest+json; charset=utf-8",
  ".svg": "image/svg+xml; charset=utf-8"
};

function sendFile(response, filePath) {
  fs.readFile(filePath, (error, content) => {
    if (error) {
      response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("Archivo no encontrado");
      return;
    }

    const extension = path.extname(filePath);
    response.writeHead(200, {
      "Content-Type": mimeTypes[extension] || "application/octet-stream",
      "Cache-Control": "no-store"
    });
    response.end(content);
  });
}

function createServer(port) {
  const server = http.createServer((request, response) => {
    const requestedPath = decodeURIComponent(new URL(request.url, `http://localhost:${port}`).pathname);
    const relativePath = requestedPath === "/" ? "index.html" : requestedPath.replace(/^\/+/, "");
    const safePath = path.normalize(relativePath);
    const filePath = path.join(root, safePath);

    if (safePath.startsWith("..") || path.isAbsolute(safePath) || !filePath.startsWith(root)) {
      response.writeHead(403, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("Acceso denegado");
      return;
    }

    sendFile(response, filePath);
  });

  server.on("error", (error) => {
    if (error.code === "EADDRINUSE" && port < basePort + 20) {
      createServer(port + 1);
      return;
    }
    throw error;
  });

  server.listen(port, "127.0.0.1", () => {
    fs.writeFileSync(path.join(root, ".server-port"), String(port));
    console.log(`Tutor Inteligente disponible en http://127.0.0.1:${port}`);
  });
}

createServer(basePort);
