package io.github.qmftm.quantummending;

/**
 * 부트스트랩과 플러그인 본체가 함께 쓰는 설정 보관함.
 */
final class ConfigHolder {

    // 서버 시작 시 읽은 설정. 인챈트 정의(최대 레벨·수리량 등)는 이 값으로 고정됩니다.
    private volatile MendingConfig startup;
    // 가장 최근에 읽은 설정. 리로드 때마다 갱신되고, 레벨 상한 리스너가 이 값을 참조합니다.
    private volatile MendingConfig current;

    MendingConfig startup() {
        return this.startup;
    }

    MendingConfig current() {
        return this.current;
    }

    void update(final MendingConfig config) {
        if (this.startup == null) {
            this.startup = config;
        }
        this.current = config;
    }
}
