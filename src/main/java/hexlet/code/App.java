package hexlet.code;

import hexlet.code.controllers.UrlController;
import hexlet.code.controllers.WelcomeController;
import io.javalin.Javalin;
import io.javalin.plugin.rendering.template.JavalinThymeleaf;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.file.WatchEvent;

import static io.javalin.apibuilder.ApiBuilder.*;

public class App {

    public static void main(String[] args) {
        Javalin app = getApp();
        app.start();
    }

    public static void addRoutes(Javalin app) {
        app.get("/", WelcomeController.welcome);

        app.routes(() -> {
            path("urls", () -> {
                get(UrlController.showAllUrls);
                post(UrlController.createUrl);
                get("{id}", UrlController.showUrl);
            });
        });
    }

    public static Javalin getApp() {
        Javalin app = Javalin.create(config -> {
            config.enableDevLogging();
            config.enableWebjars();
            JavalinThymeleaf.configure(getTemplateEngine());
        }).start(getPort());

        addRoutes(app);

        return app;
    }

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "5000");
        return Integer.valueOf(port);
    }

    public static TemplateEngine getTemplateEngine() {
        TemplateEngine templateEngine = new TemplateEngine();

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");

        templateEngine.addTemplateResolver(templateResolver);
        templateEngine.addDialect(new LayoutDialect());
        templateEngine.addDialect(new Java8TimeDialect());

        return templateEngine;
    }
}
