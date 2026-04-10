const WebSocket = require('ws');

const PORT = 8888;
const wss = new WebSocket.Server({ host: '127.0.0.1', port: PORT });

/**
 * clients: Map<deviceId, WebSocket> -> 用于物理转发
 * roleToDevice: Map<type, deviceId> -> 用于角色路由解析
 */
const clients = new Map();
const roleToDevice = new Map();

const Api = {
    INTENT_TRANSFER: "1",
    REGISTER_HOST: "2"
};

console.log(`Intent Forwarder started on port ${PORT}`);

wss.on('connection', (ws, req) => {
    const deviceId = req.url.split('/').pop();
    if (!deviceId) {
        console.error("[Conn] Rejected: No deviceId in URL");
        return ws.close(1008, "No deviceId");
    }

    if (clients.has(deviceId)) {
        console.warn(`[Conflict] Device ${deviceId} is already connected. Kicking old connection.`);

        const oldWs = clients.get(deviceId);

        try {
            oldWs.close(4001, "Kicked: Connected from another location");
        } catch (e) {
            console.error(`[Error] Failed to close old connection for ${deviceId}:`, e);
        }

        clients.delete(deviceId);
    }

    clients.set(deviceId, ws);
    console.log(`[New] Device: ${deviceId}`);

    ws.on('message', (message) => {
        try {
            const payload = JSON.parse(message);
            const api = payload.api;

            switch (api) {
                case Api.REGISTER_HOST:
                    handleRegisterHost(deviceId, payload.type);
                    break;

                case Api.INTENT_TRANSFER:
                    handleIntentTransfer(deviceId, payload);
                    break;

                default:
                    console.log(`[Unknown API] ${api} from ${deviceId}`);
            }
        } catch (e) {
            console.error(`[Error] Failed to parse JSON from ${deviceId}:`, e.message);
        }
    });

    ws.on('close', (code, reason) => {
        if (clients.get(deviceId) === ws) {
            clients.delete(deviceId);
            console.log(`[Closed] Device: ${deviceId}, Code: ${code}`);
        }
    });
});

function handleRegisterHost(deviceId, type) {
    if (!type) return;

    if (!roleToDevice.has(type)) {
        console.log(`${type} ✓  Bound:  ${deviceId}`);
    } else {
        const existingDevice = roleToDevice.get(type);
        console.log(`${type} ⚠️  Rebind: ${existingDevice} >> ${deviceId}`);
    }

    roleToDevice.set(type, deviceId);
}

function handleIntentTransfer(fromId, payload) {
    let targetId = payload.toDeviceId; // 可能是物理 UUID，也可能是 "super_host" 等 Role

    // 路由解析：Role -> 物理 DeviceID
    if (roleToDevice.has(targetId)) {
        const realId = roleToDevice.get(targetId);
        targetId = realId;
    }

    const targetWs = clients.get(targetId);
    if (targetWs && targetWs.readyState === WebSocket.OPEN) {
        const forwardData = {
            api: Api.INTENT_TRANSFER,
            from: fromId,          // 告知目标：谁发过来的
            toDeviceId: targetId,  // 物理 ID
            data: payload.data    // Base64 序列化数据
        };

        targetWs.send(JSON.stringify(forwardData));
        console.log(`[Transfer] ${fromId} >> ${targetId}`);
    } else {
        console.error(`[Route Error] Target ${targetId} is unreachable.`);
    }
}
