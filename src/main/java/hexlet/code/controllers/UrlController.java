package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.commons.validator.routines.UrlValidator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class UrlController {
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

        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;

        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());

        ctx.attribute("urls", urls);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);

        ctx.render("urls/index.html");
    };

    public static Handler createUrl = ctx -> {
        try {
            URL url = new URL(ctx.formParam("url"));
            UrlValidator urlValidator = new UrlValidator();

            if (!url.toString().contains("localhost") && !urlValidator.isValid(url.toString())) {
                throw new MalformedURLException();
            }

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

        List<UrlCheck> urlChecks = new QUrlCheck()
                .url.equalTo(url)
                .orderBy().id.desc()
                .findList();

        ctx.attribute("url", url);
        ctx.attribute("urlChecks", urlChecks);

        ctx.render("urls/show.html");
    };

    public static Handler checkUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        HttpResponse<String> response;

        try {
            response = Unirest.get(url.getName()).asString();

            int statusCode = response.getStatus();

            Document document = Jsoup.parse(response.getBody());

            String title = document.title();
            String h1 = document.select("h1").text();
            String description = document.select("meta[name=description]").attr("content");

            UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, url);
            urlCheck.save();

            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");

        } catch (UnirestException e) {
            ctx.sessionAttribute("flash", "Страница недоступна");
            ctx.sessionAttribute("flash-type", "danger");
        }

        ctx.redirect("/urls/" + id);
    };

    public static Handler deleteUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        List<UrlCheck> urlChecks = new QUrlCheck()
                .url.equalTo(url)
                .orderBy().id.asc()
                .findList();

        for (UrlCheck urlCheck : urlChecks) {
            urlCheck.delete();
        }

        url.delete();

        ctx.sessionAttribute("flash", "Страница удалена");
        ctx.sessionAttribute("flash-type", "success");

        ctx.redirect("/urls");
    };

    public static Handler checkDelete = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        UrlCheck urlCheck = new QUrlCheck()
                .id.equalTo(id)
                .findOne();

        Long urlId = urlCheck.getUrl().getId();

        urlCheck.delete();

        ctx.sessionAttribute("flash", "Проверка удалена");
        ctx.sessionAttribute("flash-type", "success");

        ctx.redirect("/urls/" + urlId);
    };
}
