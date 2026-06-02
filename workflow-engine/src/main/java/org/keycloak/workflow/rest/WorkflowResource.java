package org.keycloak.workflow.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;
import org.keycloak.workflow.model.Delegation;
import org.keycloak.workflow.model.Enums;
import org.keycloak.workflow.model.SodPolicy;
import org.keycloak.workflow.model.WorkflowDefinition;
import org.keycloak.workflow.model.WorkflowInstance;
import org.keycloak.workflow.spi.WorkflowEngineProvider;

import java.util.List;
import java.util.Map;

@Path("/workflow")
public class WorkflowResource {

    private final KeycloakSession session;

    public WorkflowResource(KeycloakSession session) { this.session = session; }

    private WorkflowEngineProvider engine() {
        return session.getProvider(WorkflowEngineProvider.class);
    }

    private String requireUser() {
        AuthResult auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
        if (auth == null) throw new jakarta.ws.rs.NotAuthorizedException("bearer token required");
        return auth.getUser().getId();
    }

    private String realmId() {
        RealmModel realm = session.getContext().getRealm();
        return realm.getId();
    }

    // ----- Embedded admin UI -----

    public static final String CONSOLE_CLIENT_ID = "workflow-console";

    /** Auto-provisions the public OIDC client used by the embedded console. */
    private void ensureConsoleClient(RealmModel realm) {
        ClientModel client = realm.getClientByClientId(CONSOLE_CLIENT_ID);
        if (client != null) return;
        client = realm.addClient(CONSOLE_CLIENT_ID);
        client.setName("Workflow Engine Console");
        client.setEnabled(true);
        client.setPublicClient(true);
        client.setStandardFlowEnabled(true);
        client.setDirectAccessGrantsEnabled(false);
        client.setProtocol("openid-connect");
        client.addRedirectUri("/realms/" + realm.getName() + "/workflow/ui*");
        client.addWebOrigin("+");
    }

    @GET
    @Path("/ui")
    @Produces(MediaType.TEXT_HTML)
    public Response ui() throws Exception {
        RealmModel realm = session.getContext().getRealm();
        ensureConsoleClient(realm);
        try (InputStream in = WorkflowResource.class.getResourceAsStream("/workflow-ui/index.html")) {
            if (in == null) return Response.status(Response.Status.NOT_FOUND).build();
            String html = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("__WF_REALM__", realm.getName())
                    .replace("__WF_CLIENT__", CONSOLE_CLIENT_ID);
            return Response.ok(html).build();
        }
    }

    // ----- Designer endpoints (FR-1) -----

    @POST
    @Path("/definitions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkflowDefinition publish(WorkflowDefinition def) {
        requireUser(); // realm-admin role check expected via policy enforcer
        def.setRealmId(realmId());
        return engine().publish(def);
    }

    @GET
    @Path("/definitions/active")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActive(@QueryParam("targetType") String tt, @QueryParam("targetId") String ti) {
        WorkflowDefinition d = engine().getActiveDefinitionFor(realmId(), tt, ti);
        return d == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(d).build();
    }

    // ----- Requests -----

    @POST
    @Path("/requests")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkflowInstance submit(Map<String, String> body) {
        String userId = requireUser();
        return engine().submit(realmId(), userId, body.get("targetType"), body.get("targetId"),
                body.get("justification"));
    }

    @GET
    @Path("/requests/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("id") String id) {
        requireUser();
        WorkflowInstance i = engine().get(id);
        return i == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(i).build();
    }

    @GET
    @Path("/requests/mine")
    @Produces(MediaType.APPLICATION_JSON)
    public List<WorkflowInstance> mine() {
        return engine().listForRequester(realmId(), requireUser());
    }

    @GET
    @Path("/requests/inbox")
    @Produces(MediaType.APPLICATION_JSON)
    public List<WorkflowInstance> inbox() {
        return engine().listForApprover(realmId(), requireUser());
    }

    @POST
    @Path("/requests/{id}/steps/{stepId}/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkflowInstance approve(@PathParam("id") String id, @PathParam("stepId") String stepId,
                                    Map<String, String> body) {
        return engine().decide(id, stepId, requireUser(),
                Enums.Decision.APPROVED, body == null ? null : body.get("comment"));
    }

    @POST
    @Path("/requests/{id}/steps/{stepId}/reject")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkflowInstance reject(@PathParam("id") String id, @PathParam("stepId") String stepId,
                                   Map<String, String> body) {
        return engine().decide(id, stepId, requireUser(),
                Enums.Decision.REJECTED, body == null ? null : body.get("comment"));
    }

    @POST
    @Path("/requests/{id}/cancel")
    public Response cancel(@PathParam("id") String id, Map<String, String> body) {
        engine().cancel(id, requireUser(), body == null ? null : body.get("reason"));
        return Response.noContent().build();
    }

    // ----- Delegations (out-of-office) -----

    @GET @Path("/delegations") @Produces(MediaType.APPLICATION_JSON)
    public List<Delegation> myDelegations() {
        return engine().listDelegations(realmId(), requireUser());
    }

    @POST @Path("/delegations") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Delegation createDelegation(Delegation d) {
        d.setRealmId(realmId());
        d.setDelegatorId(requireUser()); // can only delegate own approvals
        d.setActive(true);
        engine().saveDelegation(d);
        return d;
    }

    // ----- SoD policies (admin) -----

    @GET @Path("/sod-policies") @Produces(MediaType.APPLICATION_JSON)
    public List<SodPolicy> sodPolicies() { requireUser(); return engine().listSodPolicies(realmId()); }

    @POST @Path("/sod-policies") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public SodPolicy saveSod(SodPolicy p) {
        requireUser(); p.setRealmId(realmId()); p.setActive(true); engine().saveSodPolicy(p); return p;
    }

    // ----- Metrics dashboard -----

    @GET @Path("/metrics") @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> metrics() { requireUser(); return engine().metrics(realmId()); }
}
