/* Background push handler for DinoEater Web Admin.
 * Replace the config below with the same values used in admin.html.
 * This file must live at the site root (same folder as admin.html).
 */
importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "AIzaSyDXwT0Zs0E0ZCXUljcduH2aIvh0ntHPtLY",
  authDomain: "dinoeater-merchant.firebaseapp.com",
  databaseURL: "https://dinoeater-merchant-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "dinoeater-merchant",
  storageBucket: "dinoeater-merchant.firebasestorage.app",
  messagingSenderId: "610225727771",
  appId: "1:610225727771:web:b96a291908dc40376ccbbe"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const data = payload.data || {};
  const notification = payload.notification || {};
  const orderId = data.orderId || '';
  self.registration.showNotification(notification.title || `🔔 มีออเดอร์ใหม่ ${orderId}`, {
    body: notification.body || 'กดเพื่อเปิดดูออเดอร์',
    icon: 'https://dinoeater-merchant.web.app/icon-192.png',
    tag: 'dinoeater-new-order',
    vibrate: [200, 100, 200],
    data: { url: 'https://dinoeater-merchant.web.app/admin.html' }
  });
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const url = (event.notification.data && event.notification.data.url) || 'https://dinoeater-merchant.web.app/admin.html';
  event.waitUntil(
    clients.matchAll({ type: 'window', includeControlled: true }).then((clientList) => {
      for (const client of clientList) {
        if (client.url === url && 'focus' in client) return client.focus();
      }
      return clients.openWindow(url);
    })
  );
});
