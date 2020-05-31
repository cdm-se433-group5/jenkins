package hudson.security;

import org.acegisecurity.AccessDeniedException;
import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import jenkins.util.SystemProperties;

/**
 * {@link AccessDeniedException} with more information.
 * @author Kohsuke Kawaguchi
 */
public class AccessDeniedException2 extends AccessDeniedException {

    /** If true, report {@code X-You-Are-In-Group} headers. Disabled due to JENKINS-39402; use {@code /whoAmI} etc. to diagnose permission issues. */
    private static /* not final */ boolean REPORT_GROUP_HEADERS = SystemProperties.getBoolean(AccessDeniedException2.class.getName() + ".REPORT_GROUP_HEADERS");

    /**
     * This object represents the user being authenticated.
     */
    public final Authentication authentication;

    /**
     * This object represents the permission that the user needed.
     */
    public final Permission permission;

    public AccessDeniedException2(Authentication authentication, Permission permission) {
        this(null,authentication,permission);
    }

    public AccessDeniedException2(Throwable t, Authentication authentication, Permission permission) {
        super(Messages.AccessDeniedException2_MissingPermission(authentication.getName(),
                permission.group.title+"/"+permission.name), t);
        this.authentication = authentication;
        this.permission = permission;
    }

    /**
     * Reports the details of the access failure in HTTP headers to assist diagnosis.
     */
    public void reportAsHeaders(HttpServletResponse rsp) {
        rsp.addHeader("X-You-Are-Authenticated-As",authentication.getName());
        if (REPORT_GROUP_HEADERS) {
            for (GrantedAuthority auth : authentication.getAuthorities()) {
                rsp.addHeader("X-You-Are-In-Group",auth.getAuthority());
            }
        } else {
            rsp.addHeader("X-You-Are-In-Group-Disabled", "JENKINS-39402: use -Dhudson.security.AccessDeniedException2.REPORT_GROUP_HEADERS=true or use /whoAmI to diagnose");
        }
        rsp.addHeader("X-Required-Permission", permission.getId());
        for (Permission p=permission.impliedBy; p!=null; p=p.impliedBy) {
            rsp.addHeader("X-Permission-Implied-By", p.getId());
        }
    }

    /**
     * Reports the details of the access failure.
     * This method is similar to {@link #reportAsHeaders(HttpServletResponse)} for the intention
     * but instead of using HTTP headers, this version is meant to go inside the payload.
     */
    public void report(PrintWriter w) {
        w.println("You are authenticated as: "+authentication.getName());
        w.println("Groups that you are in:");
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            w.println("  "+auth.getAuthority());
        }

        w.println("Permission you need to have (but didn't): "+permission.getId());
        for (Permission p=permission.impliedBy; p!=null; p=p.impliedBy) {
            w.println(" ... which is implied by: "+p.getId());
        }
    }
}
