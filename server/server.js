const http = require("http");
const { WebSocketServer, WebSocket } = require("ws");

const PORT = Number(process.env.PORT || 10000);
const rooms = new Map();
const clients = new Map();
const waitingQuickMatch = [];
const ROUND_SECONDS = 60;
const MAX_PLAYERS = 2;
const CAPTURE_DISTANCE = 1.65;
const MAX_POSITION_STEP = 1.25;

const server = http.createServer((req, res) => {
  if (req.url === "/health") {
    res.writeHead(200, { "content-type": "application/json" });
    return res.end(JSON.stringify({ ok: true, service: "rooftop-hunt-server", rooms: rooms.size }));
  }
  res.writeHead(200, { "content-type": "text/plain" });
  res.end("Rooftop Hunt multiplayer server");
});

const wss = new WebSocketServer({ server, path: "/ws" });

function send(ws, payload) {
  if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(payload));
}
function broadcast(room, payload, except = null) {
  for (const player of room.players.values()) {
    if (player.ws !== except) send(player.ws, payload);
  }
}
function roomSnapshot(room) {
  return {
    type: "ROOM_READY",
    room: room.code,
    round: room.round,
    time: room.timeLeft,
    players: [...room.players.values()].map(p => ({ pid: p.pid, name: p.name, seat: p.seat }))
  };
}
function makeRoom(code) {
  return {
    code, players: new Map(), round: 1, timeLeft: ROUND_SECONDS,
    phase: "COUNTDOWN", countdown: 3, lastTick: Date.now(),
    captured: false, interval: null
  };
}
function assignRoles(room) {
  return room.round % 2 === 1
    ? { hunterSeat: 0, runnerSeat: 1 }
    : { hunterSeat: 1, runnerSeat: 0 };
}
function finishRound(room, captured) {
  if (room.phase !== "ACTIVE") return;
  room.phase = "ROUND_ENDED";
  room.captured = captured;
  broadcast(room, {
    type: "ROUND_SYNC", room: room.code, round: room.round,
    time: room.timeLeft, phase: room.phase, captured
  });
  setTimeout(() => {
    if (!rooms.has(room.code) || room.players.size !== MAX_PLAYERS) return;
    if (room.round >= 4) {
      broadcast(room, { type: "MATCH_FINISHED", room: room.code, round: 4 });
      stopRoom(room);
      return;
    }
    room.round += 1;
    room.timeLeft = ROUND_SECONDS;
    room.countdown = 3;
    room.phase = "COUNTDOWN";
    room.captured = false;
    for (const p of room.players.values()) p.lastPosition = null;
    broadcast(room, { type: "ROUND_SYNC", room: room.code, round: room.round, time: room.timeLeft, phase: room.phase });
  }, 3000);
}
function stopRoom(room) {
  if (room.interval) clearInterval(room.interval);
  rooms.delete(room.code);
}
function startRoom(room) {
  if (room.interval) clearInterval(room.interval);
  room.lastTick = Date.now();
  room.interval = setInterval(() => {
    if (room.players.size !== MAX_PLAYERS) return;
    const now = Date.now();
    const dt = Math.min((now - room.lastTick) / 1000, 0.25);
    room.lastTick = now;
    if (room.phase === "COUNTDOWN") {
      room.countdown -= dt;
      if (room.countdown <= 0) {
        room.phase = "ACTIVE";
        broadcast(room, { type: "ROUND_SYNC", room: room.code, round: room.round, time: room.timeLeft, phase: room.phase });
      }
    } else if (room.phase === "ACTIVE") {
      room.timeLeft = Math.max(0, room.timeLeft - dt);
      if (Math.floor(room.timeLeft) % 5 === 0) {
        broadcast(room, { type: "ROUND_SYNC", room: room.code, round: room.round, time: room.timeLeft, phase: room.phase });
      }
      if (room.timeLeft <= 0) finishRound(room, false);
    }
  }, 100);
}
function sendRoomReady(room) {
  const players = [...room.players.values()];
  for (const player of players) {
    const opponent = players.find(p => p.pid !== player.pid);
    send(player.ws, {
      type: "ROOM_READY", room: room.code, pid: player.pid,
      name: opponent ? opponent.name : "Opponent",
      opponentPid: opponent ? opponent.pid : "",
      seat: player.seat, round: room.round, time: room.timeLeft
    });
  }
  startRoom(room);
}
function attachPlayer(room, ws, data) {
  if (room.players.size >= MAX_PLAYERS) {
    send(ws, { type: "ROOM_ERROR", reason: "ROOM_FULL" });
    return false;
  }
  const seat = room.players.size;
  const player = {
    ws, pid: String(data.pid || "player").slice(0, 64),
    name: String(data.name || "Player").slice(0, 24),
    seat, lastPosition: null, lastUpdate: 0
  };
  room.players.set(player.pid, player);
  clients.set(ws, { room, player });
  return true;
}
function joinRoom(ws, data) {
  const code = String(data.room || "").toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 6);
  if (!code) return send(ws, { type: "ROOM_ERROR", reason: "INVALID_ROOM" });

  if (code === "MATCH" || code === "QUICK") {
    let room = waitingQuickMatch.find(r => r.players.size === 1);
    if (!room) {
      room = makeRoom("Q" + Math.random().toString(36).slice(2, 6).toUpperCase());
      rooms.set(room.code, room);
      waitingQuickMatch.push(room);
    }
    if (attachPlayer(room, ws, data) && room.players.size === 2) {
      const i = waitingQuickMatch.indexOf(room);
      if (i >= 0) waitingQuickMatch.splice(i, 1);
      sendRoomReady(room);
    } else {
      send(ws, { type: "WAITING_FOR_PLAYER", room: room.code });
    }
    return;
  }

  let room = rooms.get(code);
  if (!room) {
    if (!data.isHost) return send(ws, { type: "ROOM_ERROR", reason: "ROOM_NOT_FOUND" });
    room = makeRoom(code);
    rooms.set(code, room);
  }
  if (!attachPlayer(room, ws, data)) return;
  if (room.players.size === 1) {
    send(ws, { type: "WAITING_FOR_PLAYER", room: code });
  } else {
    sendRoomReady(room);
  }
}
function handleState(ws, data) {
  const entry = clients.get(ws);
  if (!entry) return;
  const { room, player } = entry;
  if (room.phase !== "ACTIVE") return;

  const x = Number(data.x), y = Number(data.y), z = Number(data.z);
  if (![x, y, z].every(Number.isFinite)) return;
  const now = Date.now();
  if (player.lastPosition) {
    const elapsed = Math.max((now - player.lastUpdate) / 1000, 0.02);
    const dx = x - player.lastPosition.x;
    const dy = y - player.lastPosition.y;
    const dz = z - player.lastPosition.z;
    const distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (distance > Math.max(MAX_POSITION_STEP, elapsed * 13)) return;
  }
  player.lastPosition = { x, y, z };
  player.lastUpdate = now;

  const safePacket = {
    type: "STATE_UPDATE", room: room.code, pid: player.pid,
    x, y, z,
    vx: Number(data.vx) || 0, vy: Number(data.vy) || 0, vz: Number(data.vz) || 0,
    yaw: Number(data.yaw) || 0, anim: String(data.anim || "IDLE").slice(0, 20),
    hp: 100, shield: false, speed: false, coins: 0
  };
  broadcast(room, safePacket, ws);

  const other = [...room.players.values()].find(p => p.pid !== player.pid);
  if (!other || !other.lastPosition || room.phase !== "ACTIVE") return;
  const roles = assignRoles(room);
  const hunter = player.seat === roles.hunterSeat ? player : other;
  const runner = player.seat === roles.runnerSeat ? player : other;
  if (!hunter.lastPosition || !runner.lastPosition) return;
  const dx = hunter.lastPosition.x - runner.lastPosition.x;
  const dy = hunter.lastPosition.y - runner.lastPosition.y;
  const dz = hunter.lastPosition.z - runner.lastPosition.z;
  if (dx * dx + dy * dy + dz * dz <= CAPTURE_DISTANCE * CAPTURE_DISTANCE) {
    broadcast(room, { type: "CAPTURE_EVENT", room: room.code, hunter: hunter.pid, runner: runner.pid });
    finishRound(room, true);
  }
}
wss.on("connection", ws => {
  ws.on("message", raw => {
    let data;
    try { data = JSON.parse(raw.toString()); } catch { return; }
    if (data.type === "JOIN_ROOM") return joinRoom(ws, data);
    if (data.type === "STATE_UPDATE") return handleState(ws, data);
    if (data.type === "PING") return send(ws, { type: "PONG", at: Date.now() });
  });
  ws.on("close", () => {
    const entry = clients.get(ws);
    if (!entry) return;
    const { room, player } = entry;
    clients.delete(ws);
    room.players.delete(player.pid);
    // Keep seat assignment deterministic after a disconnect/rejoin.
    [...room.players.values()].forEach((remaining, index) => { remaining.seat = index; });
    broadcast(room, { type: "OPPONENT_LEFT", room: room.code });
    if (room.players.size === 0) stopRoom(room);
    else {
      if (room.interval) clearInterval(room.interval);
      room.phase = "WAITING";
    }
  });
  ws.on("error", () => {});
});
server.listen(PORT, "0.0.0.0", () => {
  console.log(`Rooftop Hunt server listening on ${PORT}`);
});
