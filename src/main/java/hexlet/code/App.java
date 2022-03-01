package hexlet.code;

import io.javalin.Javalin;

public class App {

    public static void main(String[] args) {
        Javalin app = getApp();
        app.start();
    }

    public static Javalin getApp() {
        Javalin app = Javalin.create(config -> {
            config.enableDevLogging();
        }).start(getPort());

        app.get("/", ctx -> ctx.result("Hello World!"));

        return app;
    }

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "5000");
        return Integer.valueOf(port);
    }

}
