-- W-503 시스템 설정 초기 데이터
-- MySQL 전용. 테스트 환경(H2)에서는 spring.sql.init.mode=never 로 비활성화.
-- ddl-auto: update 환경에서 서버 기동 시 자동 실행됨
-- 이미 존재하는 키는 무시 (INSERT IGNORE)

INSERT IGNORE INTO system_setting (setting_key, setting_value, description, created_at, updated_at)
VALUES
  ('daily.send.limit',         '10000',  '회원 1인당 일일 기본 발송 한도 (건)',                   NOW(), NOW()),
  ('monthly.send.limit',       '300000', '회원 1인당 월간 기본 발송 한도 (건)',                   NOW(), NOW()),
  ('spam.block.enabled',       'true',   '스팸 키워드 자동 차단 활성화 여부 (true/false)',         NOW(), NOW()),
  ('night.ad.block.enabled',   'true',   '야간 광고성 메시지 차단 (21:00-08:00) 활성화',          NOW(), NOW()),
  ('night.ad.block.start',     '21',     '야간 광고 차단 시작 시각 (시 단위, 0-23)',              NOW(), NOW()),
  ('night.ad.block.end',       '8',      '야간 광고 차단 종료 시각 (시 단위, 0-23)',              NOW(), NOW()),
  ('balance.min.charge',       '10000',  '최소 충전 금액 (원)',                                  NOW(), NOW()),
  ('sms.unit.cost',            '9',      'SMS 건당 단가 (원)',                                   NOW(), NOW()),
  ('lms.unit.cost',            '27',     'LMS 건당 단가 (원)',                                   NOW(), NOW()),
  ('mms.unit.cost',            '55',     'MMS 건당 단가 (원)',                                   NOW(), NOW()),
  ('kakao.unit.cost',          '7',      '카카오 알림톡 건당 단가 (원)',                          NOW(), NOW()),
  ('rcs.unit.cost',            '15',     'RCS 건당 단가 (원)',                                   NOW(), NOW()),
  ('api.key.review.required',  'true',   'PRODUCTION API Key 발급 시 운영자 승인 필요 여부',       NOW(), NOW()),
  ('stats.cache.ttl.seconds',  '300',    '운영자 통계 캐시 TTL (초) — 참조용, 실제 적용은 application.yml', NOW(), NOW());
