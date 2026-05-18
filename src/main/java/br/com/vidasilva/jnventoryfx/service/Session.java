package br.com.vidasilva.jnventoryfx.service;

import br.com.vidasilva.jnventoryfx.model.User;

public final class Session {
    private static User currentUser;

    private Session() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User currentUser) {
        Session.currentUser = currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}
