package com.gym.domain.attendance.service;

/**
 * 출석 체크가 들어온 경로. 화면을 누가 보느냐에 따라 응답에 담을 정보가 달라진다.
 */
public enum CheckInChannel {
    /** 관리자·데스크 직원이 관리 화면에서 입력 */
    STAFF,
    /** 회원이 입구 키오스크에서 직접 입력. 다른 회원의 정보가 드러나지 않게 가려서 응답한다 */
    KIOSK
}
