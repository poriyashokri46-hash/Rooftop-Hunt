# Rooftop Hunt Multiplayer Server

A small WebSocket room server for the Android game.

## Deploy free prototype on Render

1. In Render, create a new **Blueprint** and select this repository.
2. Render reads `render.yaml` and creates `rooftop-hunt-server`.
3. After deployment, check `https://rooftop-hunt-server.onrender.com/health`.
4. Configure the Android app to use `wss://rooftop-hunt-server.onrender.com/ws`.

The free Render service may sleep while idle and lose in-memory rooms on restart. This is a prototype deployment, not durable production hosting.

## Protocol

- `JOIN_ROOM`: create/join a private room; `MATCH` or `QUICK` enters quick matchmaking.
- `ROOM_READY`: sent to both clients when two players are present.
- `STATE_UPDATE`: relayed to the opponent after basic position sanity checks.
- `ROUND_SYNC`: server countdown, 60-second round timer and four-round rotation.
- `CAPTURE_EVENT`: server checks hunter/runner distance before confirming capture.

The server owns room membership, seats, round order, countdown and round timer. Movement remains client-simulated with basic speed/displacement checks; this is not yet a fully authoritative physics server.
