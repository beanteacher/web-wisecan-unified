/**
 * wsc 설정 저장소 — API Key 및 base URL 을 OS 홈 디렉터리 ~/.wsc/config.json 에 보관.
 * 키체인 통합은 추후 확장 예정이며 현재는 chmod 600 파일 방식으로 보호한다.
 *
 * RQ-CLI-001~003: wsc auth login / logout / status
 */

'use strict';

const fs = require('fs');
const path = require('path');
const os = require('os');

const CONFIG_DIR = path.join(os.homedir(), '.wsc');
const CONFIG_FILE = path.join(CONFIG_DIR, 'config.json');

/** 기본 base URL — 환경변수 WSC_BASE_URL 로 재정의 가능 */
const DEFAULT_BASE_URL = 'http://localhost:8080';

/**
 * 설정 파일 읽기. 파일 없으면 빈 객체 반환.
 * @returns {{ apiKey?: string, baseUrl?: string }}
 */
function read() {
  if (!fs.existsSync(CONFIG_FILE)) {
    return {};
  }
  try {
    const raw = fs.readFileSync(CONFIG_FILE, 'utf8');
    return JSON.parse(raw);
  } catch (_) {
    return {};
  }
}

/**
 * 설정 파일 저장. 디렉터리가 없으면 생성하고 파일 권한을 600 으로 설정.
 * @param {{ apiKey?: string, baseUrl?: string }} data
 */
function write(data) {
  if (!fs.existsSync(CONFIG_DIR)) {
    fs.mkdirSync(CONFIG_DIR, { recursive: true });
  }
  fs.writeFileSync(CONFIG_FILE, JSON.stringify(data, null, 2), 'utf8');
  try {
    fs.chmodSync(CONFIG_FILE, 0o600);
  } catch (_) {
    // Windows 에서 chmod 는 무시
  }
}

/** 저장된 API Key 반환. 없으면 null. */
function getApiKey() {
  return process.env.WSC_API_KEY || read().apiKey || null;
}

/** base URL 반환. 환경변수 > 저장값 > 기본값 순 */
function getBaseUrl() {
  return process.env.WSC_BASE_URL || read().baseUrl || DEFAULT_BASE_URL;
}

/** API Key 저장 */
function saveApiKey(apiKey) {
  const cfg = read();
  cfg.apiKey = apiKey;
  write(cfg);
}

/** base URL 저장 */
function saveBaseUrl(baseUrl) {
  const cfg = read();
  cfg.baseUrl = baseUrl;
  write(cfg);
}

/** 설정 초기화 (logout) */
function clear() {
  write({});
}

/** 설정 파일 경로 반환 */
function getConfigPath() {
  return CONFIG_FILE;
}

module.exports = { getApiKey, getBaseUrl, saveApiKey, saveBaseUrl, clear, getConfigPath, read };
