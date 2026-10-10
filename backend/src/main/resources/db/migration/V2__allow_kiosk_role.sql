-- V2: 입구 출석 키오스크 전용 계정 역할(KIOSK) 허용
-- 이미 데이터가 쌓인 개발 DB 를 초기화하지 않으려고 V1 을 고치지 않고 새 파일로 추가한다.
alter table account drop constraint account_role_check;

alter table account
    add constraint account_role_check
        check (role in ('ADMIN', 'STAFF', 'TRAINER', 'MEMBER', 'KIOSK'));
