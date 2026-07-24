package com.project.bluffball.global.exception;

import org.springframework.http.HttpStatus;

/**
 * API 공통 에러 코드.
 *
 * <p>code(이름), 기본 메시지, HTTP Status를 한곳에서 관리한다.</p>
 */
public enum ErrorCode {

    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    REQUEST_BODY_EMPTY(HttpStatus.BAD_REQUEST, "요청 본문이 비어 있습니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Auth
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    AUTH_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 Refresh Token입니다."),
    AUTH_REFRESH_TOKEN_EMPTY(HttpStatus.BAD_REQUEST, "Refresh Token이 비어 있습니다."),
    AUTH_AUTHORIZATION_CODE_EMPTY(HttpStatus.BAD_REQUEST, "인가 코드가 비어 있습니다."),
    AUTH_REDIRECT_URI_EMPTY(HttpStatus.BAD_REQUEST, "redirect URI가 비어 있습니다."),
    AUTH_REDIRECT_URI_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않은 redirect URI입니다."),
    AUTH_PROVIDER_EMPTY(HttpStatus.BAD_REQUEST, "provider가 비어 있습니다."),
    AUTH_PROVIDER_UNSUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 provider입니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "접근이 거부되었습니다."),
    AUTH_CHANNEL_FORBIDDEN(HttpStatus.FORBIDDEN, "다른 유저의 개인 채널을 구독할 수 없습니다."),
    AUTH_INVALID_SUBSCRIPTION_PATH(HttpStatus.FORBIDDEN, "유효하지 않은 유저 구독 경로입니다."),
    AUTH_ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 계정입니다. 로그인할 수 없습니다."),
    AUTH_WS_REQUIRED(HttpStatus.UNAUTHORIZED, "WebSocket 인증 정보가 없습니다."),
    AUTH_WS_SESSION_MISSING(HttpStatus.UNAUTHORIZED, "WebSocket 인증 세션이 없습니다."),
    AUTH_WS_JWT_REQUIRED(HttpStatus.UNAUTHORIZED, "WebSocket 연결에 유효한 JWT가 필요합니다."),

    // Match
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭 큐에 등록되어 있지 않습니다."),
    MATCH_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "매치를 찾을 수 없습니다."),
    MATCH_ALREADY_IN_QUEUE(HttpStatus.CONFLICT, "이미 매칭 큐에 등록되어 있습니다."),
    MATCH_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "해당 매치의 참가자가 아닙니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    SOCIAL_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "소셜 계정을 찾을 수 없습니다."),

    // Card
    PITCH_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "구종 카드를 찾을 수 없습니다."),
    COORDINATE_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "좌표 카드를 찾을 수 없습니다."),
    COORDINATE_CARD_INVALID_TYPE(HttpStatus.BAD_REQUEST, "좌표 카드 유형이 올바르지 않습니다."),
    COORDINATE_CARD_MASTER_NOT_FOUND(HttpStatus.BAD_REQUEST, "좌표 카드 마스터가 없습니다."),

    // Game
    GAME_NOT_INITIALIZED(HttpStatus.BAD_REQUEST, "경기 진행이 초기화되지 않았습니다."),
    GAME_STATE_NOT_FOUND(HttpStatus.NOT_FOUND, "게임 상태를 찾을 수 없습니다."),
    TURN_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "턴 세션을 찾을 수 없습니다."),
    GAME_INVALID_TURN(HttpStatus.BAD_REQUEST, "턴 결과가 올바르지 않습니다."),
    GAME_INVALID_INNINGS(HttpStatus.BAD_REQUEST, "총 이닝 수가 올바르지 않습니다."),
    GAME_INVALID_SETUP_NUMBERS(HttpStatus.BAD_REQUEST, "블러핑 숫자가 올바르지 않습니다."),
    GAME_NOT_PITCHER(HttpStatus.BAD_REQUEST, "현재 등판 투수만 수행할 수 있습니다."),
    GAME_NOT_BATTER(HttpStatus.BAD_REQUEST, "현재 타석 타자만 수행할 수 있습니다."),
    GAME_CARD_SELECTION_INCOMPLETE(HttpStatus.BAD_REQUEST, "카드 선택이 완료되지 않았습니다."),
    GAME_CARD_NOT_IN_HAND(HttpStatus.BAD_REQUEST, "선택한 카드가 현재 패에 없습니다."),
    GAME_INVALID_COORDINATE(HttpStatus.BAD_REQUEST, "좌표 값이 올바르지 않습니다."),
    GAME_INVALID_TIMING(HttpStatus.BAD_REQUEST, "타이밍 선택이 올바르지 않습니다."),
    GAME_INVALID_RESPONSE_TIME(HttpStatus.BAD_REQUEST, "응답 시간이 올바르지 않습니다."),
    GAME_MULLIGAN_INVALID(HttpStatus.BAD_REQUEST, "멀리건 요청이 올바르지 않습니다."),
    PITCHER_RECORD_INVALID(HttpStatus.BAD_REQUEST, "투수 기록 값이 올바르지 않습니다."),
    GAME_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "이미 종료된 경기입니다."),
    GAME_NOT_ENDED(HttpStatus.BAD_REQUEST, "경기가 종료되지 않았습니다."),
    GAME_ALREADY_ARCHIVED(HttpStatus.BAD_REQUEST, "이미 DB에 저장된 경기입니다."),
    GAME_NO_TURN_RESULTS(HttpStatus.BAD_REQUEST, "저장할 턴 결과가 없습니다."),
    GAME_INCOMPLETE_TURN(HttpStatus.BAD_REQUEST, "미완료 턴이 포함되어 있습니다."),
    GAME_INCOMPLETE_PITCHER_SELECTION(HttpStatus.BAD_REQUEST, "투수 선택 정보가 없는 턴이 포함되어 있습니다."),
    GAME_INCOMPLETE_PLAYER_INFO(HttpStatus.BAD_REQUEST, "투수·타자 정보가 없는 턴이 포함되어 있습니다."),
    GAME_PITCHER_ALREADY_SELECTED(HttpStatus.BAD_REQUEST, "이미 투수 카드 선택이 완료된 턴입니다."),
    GAME_BATTER_ALREADY_SELECTED(HttpStatus.BAD_REQUEST, "이미 타자 선택이 완료된 턴입니다."),
    GAME_MULLIGAN_REQUIRED(HttpStatus.BAD_REQUEST, "카드 교체(멀리건) 확정 후에 진행할 수 있습니다."),
    GAME_MULLIGAN_ALREADY_USED(HttpStatus.BAD_REQUEST, "멀리건은 등판 당 1회만 가능합니다."),
    GAME_INGAME_MULLIGAN_UNSUPPORTED(HttpStatus.BAD_REQUEST, "현재 매치에서는 인게임 멀리건이 지원되지 않습니다."),
    GAME_PITCHER_SELECTION_REQUIRED(HttpStatus.BAD_REQUEST, "투수 카드 선택이 완료되지 않았습니다."),
    GAME_LINEUP_EMPTY(HttpStatus.BAD_REQUEST, "타순이 비어 있습니다."),
    GAME_DOUBLE_JUDGMENT_NOT_CONFIGURED(HttpStatus.BAD_REQUEST, "2루타 판정 설정이 없습니다."),
    GAME_SETUP_NUMBERS_MISSING(HttpStatus.BAD_REQUEST, "블러핑 숫자가 제출되지 않았습니다."),
    GAME_CARD_POOL_INSUFFICIENT(HttpStatus.BAD_REQUEST, "구종 카드 수가 핸드 장수보다 적습니다."),
    GAME_CARD_REDRAW_INSUFFICIENT(HttpStatus.BAD_REQUEST, "재뽑기 풀의 카드가 부족합니다."),
    GAME_ROLE_SWAP_UNSUPPORTED(HttpStatus.BAD_REQUEST, "현재 매치에서는 공수 교대 역할 교환이 지원되지 않습니다."),

    // Team
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "팀을 찾을 수 없습니다."),
    TEAM_NAME_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 팀 이름입니다."),
    TEAM_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 팀에 소속되어 있습니다."),
    TEAM_NOT_MEMBER(HttpStatus.FORBIDDEN, "해당 팀의 멤버가 아닙니다."),
    TEAM_FORBIDDEN(HttpStatus.FORBIDDEN, "팀 리더만 수행할 수 있습니다."),
    TEAM_LEADER_CANNOT_LEAVE(HttpStatus.CONFLICT, "팀 리더는 탈퇴할 수 없습니다. 리더를 위임한 뒤 탈퇴하세요."),
    TEAM_LEADER_CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "팀 리더 자신은 강제 탈퇴할 수 없습니다."),
    TEAM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "팀 멤버를 찾을 수 없습니다."),
    TEAM_CURRENCY_INSUFFICIENT(HttpStatus.BAD_REQUEST, "보유 재화가 부족합니다."),
    TEAM_DONATE_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "기부 금액이 올바르지 않습니다."),
    TEAM_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 시즌의 팀 기록을 찾을 수 없습니다."),
    TEAM_NAME_INVALID(HttpStatus.BAD_REQUEST, "팀 이름이 올바르지 않습니다."),

    // League
    LEAGUE_NOT_FOUND(HttpStatus.NOT_FOUND, "리그를 찾을 수 없습니다."),
    LEAGUE_SEASON_NOT_FOUND(HttpStatus.NOT_FOUND, "리그 시즌을 찾을 수 없습니다."),
    LEAGUE_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "모집 중인 시즌이 아닙니다."),
    LEAGUE_TICKET_ALREADY_OWNED(HttpStatus.CONFLICT, "이미 해당 시즌 참여권을 보유하고 있습니다."),
    LEAGUE_TICKET_NOT_FOUND(HttpStatus.BAD_REQUEST, "리그 참여권이 없습니다."),
    LEAGUE_TREASURY_INSUFFICIENT(HttpStatus.BAD_REQUEST, "팀 재정이 부족하여 참여권을 구매할 수 없습니다."),
    LEAGUE_MEMBERS_INSUFFICIENT(HttpStatus.BAD_REQUEST, "리그 참가에 필요한 최소 팀원 수가 부족합니다."),
    LEAGUE_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 해당 시즌에 참가 중입니다."),
    LEAGUE_SEASON_FULL(HttpStatus.CONFLICT, "시즌 참가 정원이 초과되었습니다."),
    LEAGUE_AFFILIATION_NOT_FOUND(HttpStatus.NOT_FOUND, "현재 소속된 리그가 없습니다."),

    // External
    OAUTH_API_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "소셜 로그인 서비스에 연결할 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
