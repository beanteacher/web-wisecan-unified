/**
 * wsc HTTP 클라이언트 — Node.js 내장 https/http 모듈 사용 (외부 의존성 없음).
 *
 * 인증: X-API-Key 헤더 (ApiKeyAuthFilter 와 동일)
 * 종료 코드 표준 (02 §7.2):
 *   0 성공 / 1 인증 오류 / 2 잔액 부족 / 3 발송 오류 / 4 입력 오류 / 5 네트워크 오류
 */

'use strict';

const https = require('https');
const http = require('http');
const store = require('../config/store');

/**
 * HTTP 요청 실행.
 * @param {'GET'|'POST'|'PATCH'|'DELETE'} method
 * @param {string} path  — '/dispatch/send' 등 경로
 * @param {object|null} body  — JSON 직렬화 대상
 * @returns {Promise<{ status: number, body: any }>}
 */
function request(method, path, body) {
  const baseUrl = store.getBaseUrl();
  const apiKey = store.getApiKey();

  const url = new URL(path, baseUrl);
  const isHttps = url.protocol === 'https:';
  const transport = isHttps ? https : http;

  const payload = body ? JSON.stringify(body) : null;

  const options = {
    hostname: url.hostname,
    port: url.port || (isHttps ? 443 : 80),
    path: url.pathname + url.search,
    method,
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    },
  };

  if (apiKey) {
    options.headers['X-API-Key'] = apiKey;
  }
  if (payload) {
    options.headers['Content-Length'] = Buffer.byteLength(payload);
  }

  return new Promise((resolve, reject) => {
    const req = transport.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        let parsed;
        try {
          parsed = JSON.parse(data);
        } catch (_) {
          parsed = data;
        }
        resolve({ status: res.statusCode, body: parsed });
      });
    });

    req.on('error', (err) => reject(err));

    if (payload) {
      req.write(payload);
    }
    req.end();
  });
}

/** GET 요청 */
function get(path) {
  return request('GET', path, null);
}

/** POST 요청 */
function post(path, body) {
  return request('POST', path, body);
}

/** PATCH 요청 */
function patch(path, body) {
  return request('PATCH', path, body);
}

/** DELETE 요청 */
function del(path) {
  return request('DELETE', path, null);
}

/**
 * HTTP 응답 상태 코드를 CLI 종료 코드로 변환.
 * @param {number} status
 * @returns {number}
 */
function exitCodeFromStatus(status) {
  if (status === 401 || status === 403) return 1; // 인증 오류
  if (status === 402)                   return 2; // 잔액 부족
  if (status >= 500)                    return 3; // 서버/발송 오류
  if (status === 400 || status === 422) return 4; // 입력 오류
  return 0;
}

module.exports = { get, post, patch, del, exitCodeFromStatus };
