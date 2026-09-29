import "dotenv/config";
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import express from "express";
import jwt from "jsonwebtoken";
import multer from "multer";

const here = path.dirname(fileURLToPath(import.meta.url));
const dataDirectory = path.join(here, "data");
const mediaDirectory = path.join(dataDirectory, "media");
const storePath = path.join(dataDirectory, "store.json");
fs.mkdirSync(mediaDirectory, { recursive: true });

const env = {
  port: Number(process.env.PORT || 3000),
  adminPassword: process.env.ADMIN_PASSWORD,
  jwtSecret: process.env.JWT_SECRET,
  enrollmentCode: process.env.ENROLLMENT_CODE
};
for (const [key, value] of Object.entries(env)) {
  if (!value && key !== "port") throw new Error(`${key} .env dosyasinda zorunludur.`);
}

function loadStore() {
  if (!fs.existsSync(storePath)) return { videos: [], devices: [] };
  return JSON.parse(fs.readFileSync(storePath, "utf8"));
}
function saveStore(store) {
  const temporaryPath = `${storePath}.tmp`;
  fs.writeFileSync(temporaryPath, JSON.stringify(store, null, 2));
  fs.renameSync(temporaryPath, storePath);
}
function defaultPolicy() {
  return { enabled: true, playbackEnabled: true, forceLock: false, maxDailyMinutes: 0, message: "", blockedVideoIds: [] };
}

const app = express();
app.use(express.json({ limit: "1mb" }));
app.get("/admin", (_request, response) => response.sendFile(path.join(here, "public", "admin.html")));
app.use(express.static(path.join(here, "public")));
const upload = multer({
  dest: path.join(dataDirectory, "uploads"),
  limits: { fileSize: 4 * 1024 * 1024 * 1024 },
  fileFilter: (_request, file, callback) => callback(null, file.mimetype.startsWith("video/"))
});

function adminOnly(request, response, next) {
  try {
    const token = request.headers.authorization?.replace("Bearer ", "");
    request.admin = jwt.verify(token, env.jwtSecret);
    next();
  } catch {
    response.status(401).json({ error: "Yonetici oturumu gerekli." });
  }
}
function deviceOnly(request, response, next) {
  const token = request.headers.authorization?.replace("Bearer ", "");
  const store = loadStore();
  const device = store.devices.find((entry) => entry.accessToken === token);
  if (!device) return response.status(401).json({ error: "Gecersiz cihaz erisimi." });
  request.store = store;
  request.device = device;
  next();
}

app.post("/api/admin/login", (request, response) => {
  if (request.body.password !== env.adminPassword) return response.status(401).json({ error: "Parola dogru degil." });
  response.json({ token: jwt.sign({ role: "admin" }, env.jwtSecret, { expiresIn: "8h" }) });
});

app.post("/api/devices/register", (request, response) => {
  const { installationId, name, enrollmentCode } = request.body;
  if (!installationId || enrollmentCode !== env.enrollmentCode) return response.status(401).json({ error: "Kayit kodu gecersiz." });
  const store = loadStore();
  let device = store.devices.find((entry) => entry.installationId === installationId);
  if (!device) {
    device = { id: crypto.randomUUID(), installationId, name: String(name || "Android cihaz").slice(0, 80), accessToken: crypto.randomBytes(32).toString("hex"), policy: defaultPolicy(), createdAt: new Date().toISOString() };
    store.devices.push(device);
  }
  device.lastSeenAt = new Date().toISOString();
  saveStore(store);
  response.json({ accessToken: device.accessToken, policy: device.policy });
});

app.get("/api/device/config", deviceOnly, (request, response) => {
  request.device.lastSeenAt = new Date().toISOString();
  saveStore(request.store);
  response.json({ policy: request.device.policy });
});

app.get("/api/catalog", deviceOnly, (request, response) => {
  const blocked = new Set(request.device.policy.blockedVideoIds || []);
  response.json({
    version: 1,
    updatedAt: new Date().toISOString(),
    videos: request.store.videos.filter((video) => video.published && !blocked.has(video.id)).map((video) => ({
      ...video,
      streamUrl: `${request.protocol}://${request.get("host")}/api/media/${video.id}`
    }))
  });
});

app.get("/api/media/:id", deviceOnly, (request, response) => {
  const policy = request.device.policy;
  if (!policy.enabled || !policy.playbackEnabled || policy.forceLock || policy.blockedVideoIds?.includes(request.params.id)) return response.sendStatus(403);
  const video = request.store.videos.find((entry) => entry.id === request.params.id && entry.published);
  if (!video) return response.sendStatus(404);
  const source = path.join(mediaDirectory, video.fileName);
  if (!fs.existsSync(source)) return response.sendStatus(404);
  const size = fs.statSync(source).size;
  const range = request.headers.range;
  response.setHeader("Content-Type", video.mimeType || "video/mp4");
  response.setHeader("Accept-Ranges", "bytes");
  if (!range) {
    response.setHeader("Content-Length", size);
    return fs.createReadStream(source).pipe(response);
  }
  const [startText, endText] = range.replace("bytes=", "").split("-");
  const start = Number(startText);
  const end = endText ? Number(endText) : size - 1;
  if (!Number.isFinite(start) || start >= size || end < start) return response.status(416).set("Content-Range", `bytes */${size}`).end();
  response.status(206).set({ "Content-Range": `bytes ${start}-${Math.min(end, size - 1)}/${size}`, "Content-Length": Math.min(end, size - 1) - start + 1 });
  fs.createReadStream(source, { start, end: Math.min(end, size - 1) }).pipe(response);
});

app.get("/api/admin/videos", adminOnly, (_request, response) => response.json(loadStore().videos));
app.post("/api/admin/videos", adminOnly, upload.single("video"), (request, response) => {
  if (!request.file || !request.body.title) return response.status(400).json({ error: "Baslik ve video zorunludur." });
  const id = crypto.randomUUID();
  const extension = path.extname(request.file.originalname).toLowerCase() || ".mp4";
  const fileName = `${id}${extension}`;
  fs.renameSync(request.file.path, path.join(mediaDirectory, fileName));
  const store = loadStore();
  const video = { id, title: String(request.body.title).slice(0, 120), description: String(request.body.description || "").slice(0, 800), category: String(request.body.category || "Videolar").slice(0, 60), fileName, mimeType: request.file.mimetype, published: true, createdAt: new Date().toISOString() };
  store.videos.push(video);
  saveStore(store);
  response.status(201).json(video);
});
app.delete("/api/admin/videos/:id", adminOnly, (request, response) => {
  const store = loadStore();
  const index = store.videos.findIndex((entry) => entry.id === request.params.id);
  if (index === -1) return response.sendStatus(404);
  const [video] = store.videos.splice(index, 1);
  fs.rmSync(path.join(mediaDirectory, video.fileName), { force: true });
  saveStore(store);
  response.sendStatus(204);
});
app.get("/api/admin/devices", adminOnly, (_request, response) => response.json(loadStore().devices));
app.patch("/api/admin/devices/:id/policy", adminOnly, (request, response) => {
  const store = loadStore();
  const device = store.devices.find((entry) => entry.id === request.params.id);
  if (!device) return response.sendStatus(404);
  const policy = request.body;
  device.policy = { enabled: Boolean(policy.enabled), playbackEnabled: Boolean(policy.playbackEnabled), forceLock: Boolean(policy.forceLock), maxDailyMinutes: Math.max(0, Math.min(1440, Number(policy.maxDailyMinutes) || 0)), message: String(policy.message || "").slice(0, 240), blockedVideoIds: Array.isArray(policy.blockedVideoIds) ? policy.blockedVideoIds.filter((id) => typeof id === "string") : [] };
  saveStore(store);
  response.json(device.policy);
});

app.listen(env.port, () => console.log(`Naz Tube sunucusu ${env.port} portunda calisiyor.`));
