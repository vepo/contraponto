package dev.vepo.contraponto.blog;

import java.util.List;

import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.notification.BlogAudienceView;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.seo.SeoMetadata;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.share.ShareView;
import dev.vepo.contraponto.tag.AuthorTagUsage;
import dev.vepo.contraponto.tag.TagUsage;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.User;

public record BlogHomeView(User author,
                           Blog mainBlog,
                           Page<Post> posts,
                           List<TagUsage> topTags,
                           List<AuthorTagUsage> mainAuthors,
                           long totalAuthors,
                           Links links,
                           LoggedUser user,
                           BlogAudienceView audience,
                           ShareView share,
                           BreadcrumbTrail breadcrumb,
                           SeoMetadata seo) {}
