package com.refound.auth.infrastructure;

/** Xác minh token đăng nhập từ app. Bản thật: {@link FirebaseTokenVerifier}. */
public interface IdentityVerifier {

    /** @throws InvalidIdentityTokenException token sai chữ ký, sai project, hết hạn hoặc không phải đăng nhập Google */
    VerifiedIdentity verify(String idToken);

    /** {@code uid} = claim {@code sub} của Firebase; {@code name} là tên Google, có thể null. */
    record VerifiedIdentity(String uid, String name) {}

    class InvalidIdentityTokenException extends RuntimeException {
        public InvalidIdentityTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
