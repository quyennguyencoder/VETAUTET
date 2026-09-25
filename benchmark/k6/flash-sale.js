/**
 * Flash Sale Benchmark — CAS vs MQ
 *
 * Chạy CAS:
 *   k6 run flash-sale.js
 *
 * Chạy MQ (khi có):
 *   k6 run flash-sale.js -e ENDPOINT=/order/mq
 *
 * Tùy chỉnh:
 *   k6 run flash-sale.js -e TICKET_ID=3 -e STOCK=2000 -e TOTAL_USERS=2000
 */

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// ---------- Custom Metrics ----------
const successOrders   = new Counter('orders_success');      // đặt thành công
const outOfStockCount = new Counter('orders_out_of_stock'); // hết vé (nghiệp vụ)
const errorCount      = new Counter('orders_error');        // lỗi server / parse
const orderLatency    = new Trend('order_latency_ms', true);

// ---------- Config ----------
const BASE_URL    = __ENV.BASE_URL    || 'http://localhost:1122';
const TICKET_ID   = parseInt(__ENV.TICKET_ID   || '3');
const QUANTITY    = parseInt(__ENV.QUANTITY    || '1');
const ENDPOINT    = __ENV.ENDPOINT    || '/order/cas';   // thay /order/mq để so sánh
const STOCK       = parseInt(__ENV.STOCK       || '2000');
const TOTAL_USERS = parseInt(__ENV.TOTAL_USERS || '100000'); // requests gửi đến server, không phải VUs
// VUS: CAS bị giới hạn bởi pool (50) → dùng 80~100
//      MQ trả về ngay          → dùng 300~500
const VUS         = parseInt(__ENV.VUS         || '500');

// ---------- Scenarios ----------
export const options = {
  scenarios: {
    flash_rush: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: TOTAL_USERS,
      maxDuration: '120s',
    },
  },
  thresholds: {
    // Latency — chấp nhận p95 < 3s dưới tải flash sale
    'http_req_duration': ['p(95)<3000', 'p(99)<5000'],
    // HTTP error (5xx, timeout) < 1% — OUT_OF_STOCK trả về 200 nên không tính vào đây
    'http_req_failed': ['rate<0.01'],
    // Oversell guard: số đơn thành công KHÔNG được vượt quá stock ban đầu
    'orders_success': [`count<=${STOCK}`],
  },
  summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// ---------- Setup: warm cache ----------
export function setup() {
  const res = http.get(`${BASE_URL}/ticket/${TICKET_ID}`);
  const ok = check(res, { 'setup: ticket exists': (r) => r.status === 200 });
  if (!ok) {
    console.error(`[SETUP FAILED] Cannot warm cache for ticketId=${TICKET_ID}, status=${res.status}`);
  } else {
    console.log(`[SETUP OK] ticketId=${TICKET_ID} | stock=${STOCK} | endpoint=${ENDPOINT} | vus=${VUS} | totalUsers=${TOTAL_USERS}`);
  }
}

// ---------- Main ----------
export default function () {
  const payload = JSON.stringify({ ticketId: TICKET_ID, quantity: QUANTITY });
  const params  = { headers: { 'Content-Type': 'application/json' } };

  const start = Date.now();
  const res   = http.post(`${BASE_URL}${ENDPOINT}`, payload, params);
  orderLatency.add(Date.now() - start);

  const httpOk = check(res, { 'HTTP 200': (r) => r.status === 200 });
  if (!httpOk) {
    errorCount.add(1);
    console.warn(`[ERROR] HTTP ${res.status}: ${res.body?.substring(0, 200)}`);
    return;
  }

  let success, code, msg;
  try {
    success = res.json('success');
    code = res.json('code');
    msg = res.json('message');
  } catch (_) {
    errorCount.add(1);
    console.warn(`[ERROR] Cannot parse JSON: ${res.body?.substring(0, 200)}`);
    return;
  }

  if (success === true) {
    successOrders.add(1);
  } else if (code === 400 && msg && msg.includes('Hết vé')) {
    outOfStockCount.add(1);
  } else {
    errorCount.add(1);
    console.warn(`[UNEXPECTED] code=${code} | msg=${msg}`);
  }
}

// ---------- Summary ----------
export function handleSummary(data) {
  const m         = data.metrics;
  const success   = m.orders_success?.values?.count     || 0;
  const oos       = m.orders_out_of_stock?.values?.count || 0;
  const errors    = m.orders_error?.values?.count        || 0;
  const total     = success + oos + errors;
  const rps       = (m.http_reqs?.values?.rate           || 0).toFixed(1);
  const p95       = (m.http_req_duration?.values?.['p(95)'] || 0).toFixed(0);
  const p99       = (m.http_req_duration?.values?.['p(99)'] || 0).toFixed(0);
  const oversell  = success > STOCK ? `❌ OVERSOLD! (${success} > ${STOCK})` : `✅ OK (${success} ≤ ${STOCK})`;

  const summary = `
╔══════════════════════════════════════════════════════════╗
║            FLASH SALE BENCHMARK — RESULT                 ║
╠══════════════════════════════════════════════════════════╣
║  Endpoint       : ${ENDPOINT.padEnd(40)}║
║  Ticket ID      : ${String(TICKET_ID).padEnd(40)}║
║  Stock          : ${String(STOCK).padEnd(40)}║
║  Total Requests : ${String(TOTAL_USERS).padEnd(40)}║
╠══════════════════════════════════════════════════════════╣
║  ✅ Đặt thành công  : ${String(success).padEnd(37)}║
║  🚫 Hết vé          : ${String(oos).padEnd(37)}║
║  ❌ Lỗi server      : ${String(errors).padEnd(37)}║
║  📦 Tổng xử lý      : ${String(total).padEnd(37)}║
╠══════════════════════════════════════════════════════════╣
║  Throughput (RPS)   : ${String(rps).padEnd(37)}║
║  Latency p95 (ms)   : ${String(p95).padEnd(37)}║
║  Latency p99 (ms)   : ${String(p99).padEnd(37)}║
╠══════════════════════════════════════════════════════════╣
║  Oversell Check     : ${oversell.padEnd(37)}║
╚══════════════════════════════════════════════════════════╝
`;

  // console.log(summary);
  return { stdout: summary };
}