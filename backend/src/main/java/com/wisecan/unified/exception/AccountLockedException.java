package com.wisecan.unified.exception;

public class AccountLockedException extends RuntimeException {

    public AccountLockedException() {
        super("로그인 5회 실패로 계정이 15분간 잠금되었습니다. 잠시 후 다시 시도해 주세요.");
    }

    public AccountLockedException(String message) {
        super(message);
    }
}
