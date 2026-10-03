package com.cuymonitor.backend.domain.port.in;

public record RegisterUserCommand (String username, String fullName, String email, String password) {

    @Override
    public String toString() { return "username: " + username + ", email: " + email + ", password: ***";}
}
