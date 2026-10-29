-- 사용자 작성 콘텐츠(모임 제목/소개, 후기, 채팅, 프로필)의 한국어 번역 캐시.
-- 지금까지는 원문이 항상 한국어라고 보고 *_EN/*_JA 번역 캐시만 두었는데, 영어/일본어로 쓴 글은
-- 원문 그대로 한국어 화면에 보였다. 같은 방식으로 *_KO 캐시를 추가한다.
--  - 원문 언어는 별도 컬럼 없이 글자 자체로 판별한다(ContentLangUtil.detect, 기존 데이터도 그대로 판별됨).
--  - 원문과 같은 언어의 캐시는 쓰지 않는다(원문 표시). 기존 컬럼/값은 건드리지 않는다(추가만, 모두 NULL 허용).
--  - 원문이 바뀌는 수정 경로(프로필 소개)는 기존과 같이 수정 시 해당 행의 번역 캐시를 모두 비운다.
ALTER TABLE TB_TRMA_MOIM_LIST    ADD COLUMN IF NOT EXISTS MOIM_TITLE_KO     VARCHAR(200) NULL;
ALTER TABLE TB_TRMA_MOIM_LIST    ADD COLUMN IF NOT EXISTS MOIM_DSCR_KO      TEXT NULL;
ALTER TABLE TB_TRMA_MOIM_REVIEW  ADD COLUMN IF NOT EXISTS REVIEW_TITLE_KO   VARCHAR(200) NULL;
ALTER TABLE TB_TRMA_MOIM_REVIEW  ADD COLUMN IF NOT EXISTS REVIEW_CONTENT_KO TEXT NULL;
ALTER TABLE TB_TRMA_CHAT_MESSAGE ADD COLUMN IF NOT EXISTS CONTENT_KO        TEXT NULL;
ALTER TABLE TB_TRMA_USER_INFO    ADD COLUMN IF NOT EXISTS AREA_NM_KO        VARCHAR(200) NULL;
ALTER TABLE TB_TRMA_USER_INFO    ADD COLUMN IF NOT EXISTS DESCRIPTION_KO    TEXT NULL;
