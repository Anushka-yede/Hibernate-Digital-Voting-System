import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let client;

function wsUrl() {
  const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
  return `${apiBase}/ws`;
}

export function connectRealtime() {
  if (client?.active) {
    return client;
  }

  client = new Client({
    webSocketFactory: () => new SockJS(wsUrl()),
    reconnectDelay: 3000,
    debug: () => {}
  });

  client.activate();
  return client;
}

export function subscribeRealtime(destination, callback) {
  const activeClient = connectRealtime();
  let subscription;
  let cancelled = false;

  const attachWhenReady = () => {
    if (cancelled) {
      return;
    }
    if (!activeClient.connected) {
      setTimeout(attachWhenReady, 200);
      return;
    }
    subscription = activeClient.subscribe(destination, (message) => {
      try {
        callback(JSON.parse(message.body));
      } catch {
        callback(message.body);
      }
    });
  };

  attachWhenReady();
  return () => {
    cancelled = true;
    if (subscription) {
      subscription.unsubscribe();
    }
  };
}

export function disconnectRealtime() {
  if (client?.active) {
    client.deactivate();
  }
}
