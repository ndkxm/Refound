package com.refound.claim.domain;

/** BR-05 (05 §5.3): số lần bị từ chối tối đa trên một tin theo uy tín. Rút ({@code withdraw}) không bị đếm. */
public final class AttemptPolicy {

    private AttemptPolicy() {}

    public static int maxAttempts(int reputation) {
        if (reputation < 0) {
            return 1;
        }
        return reputation < 30 ? 3 : 5;
    }

    /** @param rejectedCount số claim {@code rejected} của người này trên tin này */
    public static boolean canSubmit(int reputation, int rejectedCount) {
        return rejectedCount < maxAttempts(reputation);
    }
}
