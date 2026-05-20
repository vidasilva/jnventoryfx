package br.com.vidasilva.jnventoryfx.security;

import br.com.vidasilva.jnventoryfx.model.User;
import br.com.vidasilva.jnventoryfx.service.AuditService;
import br.com.vidasilva.jnventoryfx.service.Session;

public final class AuthorizationService {

    private AuthorizationService() {
    }

    public static boolean currentUserCan(Permission permission) {
        User currentUser = Session.getCurrentUser();
        return currentUser != null && currentUser.getRole() != null && currentUser.getRole().hasPermission(permission);
    }

    public static void require(Permission permission) {
        if (!currentUserCan(permission)) {
            AuditService.record(
                    "PERMISSION_DENIED",
                    "PERMISSION",
                    permission == null ? "" : permission.name(),
                    "DENIED",
                    permission == null ? "Missing permission." : "Denied permission to " + permission.getLabel() + "."
            );
            throw new SecurityException("You do not have permission to " + permission.getLabel() + ".");
        }
    }
}
