const WebSocket = require('ws');

const PORT = 8888;
const wss = new WebSocket.Server({ port: PORT });

/**
 * 数据结构说明：
 * clients: Map<deviceId, WebSocket> -> 用于物理转发
 * roleToDevice: Map<type, deviceId> -> 用于角色路由解析
 */
const clients = new Map();
const roleToDevice = new Map();

// 对齐客户端 WSClient.Api 内部类
const Api = {
    INTENT_TRANSFER: "1",
    REGISTER_HOST: "2"
};

console.log(`Intent Forwarder started on port ${PORT}`);

wss.on('connection', (ws, req) => {
    // 1. 获取 DeviceId: 对应 Request.Builder().url(sServerUrl + deviceId)
    const deviceId = req.url.split('/').pop();
    if (!deviceId) {
        console.error("[Conn] Rejected: No deviceId in URL");
        return ws.close();
    }

    clients.set(deviceId, ws);
    console.log(`[Connected] Device: ${deviceId}`);

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

    ws.on('close', () => {
        clients.delete(deviceId);
        console.log(`[Disconnected] Device: ${deviceId}`);
    });
});

/**
 * 对应客户端 registerHost 逻辑
 */
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

/**
 * 对应客户端 sendIntent 逻辑与 parseAndDispatch 逻辑
 */
function handleIntentTransfer(fromId, payload) {
    let targetId = payload.toDeviceId; // 可能是物理 UUID，也可能是 "super_host" 等 Role

    // 路由解析：Role -> 物理 DeviceID
    if (roleToDevice.has(targetId)) {
        const realId = roleToDevice.get(targetId);
        targetId = realId;
    }

    const targetWs = clients.get(targetId);
    if (targetWs && targetWs.readyState === WebSocket.OPEN) {
        // 构建发往目标端的 JSON，确保字段与客户端 parseAndDispatch 对齐
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
