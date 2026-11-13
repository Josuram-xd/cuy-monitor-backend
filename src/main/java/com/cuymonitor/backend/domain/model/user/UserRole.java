package com.cuymonitor.backend.domain.model.user;

/**
 * What an account is allowed to do. There used to be one kind of user on purpose; the catalog of breeds and
 * colors lives in the database so it can change without a release, and somebody has to be allowed to change it.
 */
public enum UserRole {
    /** Looks after their own cages. Everything the app did before. */
    BREEDER,
    /** Also edits the catalog every breeder picks from, and the accounts. */
    ADMIN;

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
