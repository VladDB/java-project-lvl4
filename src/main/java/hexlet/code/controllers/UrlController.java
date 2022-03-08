package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.PagedList;
import io.javalin.http.Handler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class UrlController {
    public static Handler showAllUrls = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        int rowsPerPage = 10;
        int offset = (page - 1) * rowsPerPage;

        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow(offset)
                .setMaxRows(rowsPerPage)
                .orderBy()
                    .id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();

        ctx.attribute("urls", urls);
        ctx.attribute("page", page);
        ctx.render("urls/index.html");
    };

    public static Handler createUrl = ctx -> {
        try {
            URL url = new URL(ctx.formParam("url"));
            String normalUrl = url.getProtocol() + "://" + url.getAuthority();

            boolean checkUrlInDB = new QUrl()
                    .name.equalTo(normalUrl)
                    .exists();

            if (checkUrlInDB) {
                ctx.sessionAttribute("flash", "Страница уже существует");
                ctx.sessionAttribute("flash-type", "info");
            } else {
                new Url(normalUrl).save();
                ctx.sessionAttribute("flash", "Страница успешно добавлена");
                ctx.sessionAttribute("flash-type", "success");
            }

        } catch (MalformedURLException e) {
            ctx.sessionAttribute("flash", "Некорректно указан URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
            return;
        }
        ctx.redirect("/urls");
    };

    public static Handler showUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };
}
