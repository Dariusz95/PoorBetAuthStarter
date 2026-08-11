package com.poorbet.authstarter.security;

public final class PoorbetPermissions {

    public static final String COUPON_CREATE = "coupon:create";
    public static final String TEAM_CREATE = "team:create";
    public static final String TEAM_READ = "team:read";
    public static final String TEAM_UPDATE = "team:update";
    public static final String MATCH_ODDS_READ = "match:odds:read";
    public static final String INTERNAL_READ = "internal:read";
    public static final String INTERNAL_WRITE = "internal:write";

    private PoorbetPermissions() {
    }
}
