package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {

    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    private static Javalin app;
    private static String baseUrl;
    private static Transaction transaction;
    private static Url exampleUrl;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;

        exampleUrl = new Url("https://example.com");
        exampleUrl.save();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() {
        transaction.rollback();
    }

    @Nested
    class MainTest {

        @Test
        void testStart() {
            HttpResponse<String> response = Unirest.get(baseUrl).asString();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains("Анализатор страниц");
        }

        @Test
        void testShowAllUrls() {
            HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains(exampleUrl.getName());
        }

        @Test
        void addInvalidUrl() {
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", "test")
                    .asEmpty();

            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

            HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();

            assertThat(response.getBody()).contains("Некорректно указан URL");
        }

        @Test
        void similarUrl() {
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", exampleUrl.getName())
                    .asEmpty();

            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();

            assertThat(response.getBody()).contains("Страница уже существует");
        }

        @Test
        void addValidUrl() {
            String testValue = "https://test.com";

            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", testValue)
                    .asEmpty();

            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();

            assertThat(response.getBody()).contains(testValue);
            assertThat(response.getBody()).contains("Страница успешно добавлена");

            Url testUrl = new QUrl()
                    .name.equalTo(testValue)
                    .findOne();

            assertThat(testUrl).isNotNull();
            assertThat(testUrl.getName()).isEqualTo(testValue);
        }
    }
}
