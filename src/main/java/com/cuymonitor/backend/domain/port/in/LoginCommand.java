package com.cuymonitor.backend.domain.port.in;

public record LoginCommand (String username, String password) {

    @Override
    public String toString(){
        return "username: " + username + ", password: ***";
    }
}
