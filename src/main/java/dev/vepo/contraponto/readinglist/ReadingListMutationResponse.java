package dev.vepo.contraponto.readinglist;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class ReadingListMutationResponse {

    private static boolean isHubTarget(ContainerRequestContext requestContext) {
        String target = requestContext.getHeaderString("HX-Target");
        return target != null && target.contains("savedListContent");
    }

    private final ReadingListComponentEndpoint componentEndpoint;

    private final ReadingListHubEndpoint hubEndpoint;

    @Inject
    public ReadingListMutationResponse(ReadingListComponentEndpoint componentEndpoint,
                                       ReadingListHubEndpoint hubEndpoint) {
        this.componentEndpoint = componentEndpoint;
        this.hubEndpoint = hubEndpoint;
    }

    public Response build(Toast.ToastResponseBuilder toast,
                          Post post,
                          String tab,
                          int page,
                          ContainerRequestContext requestContext) {
        if (isHubTarget(requestContext)) {
            String activeTab = tab != null ? tab : "unread";
            return toast.html(hubEndpoint.renderTab(activeTab, page, "/reading/saved").render()).build();
        }
        return toast.page(componentEndpoint.renderReadingList(post)).build();
    }
}
