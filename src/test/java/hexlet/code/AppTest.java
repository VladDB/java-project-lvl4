package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    private static MockWebServer mockWebServer;

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;

        exampleUrl = new Url("https://example.com");
        exampleUrl.save();

        mockWebServer = new MockWebServer();
        String mockConfig = Files.readString(Paths.get("src", "test", "resources", "mockTest"));
        mockWebServer.enqueue(new MockResponse().setBody(mockConfig));
        mockWebServer.start();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        app.stop();
        mockWebServer.shutdown();
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

        @Test
        void urlCheckTest() {
            String expectTitle = "Хекслет — больше чем школа";
            String expectH1 = "Онлайн-школа программирования";
            String expectDescription = "Живое онлайн сообщество программистов";

            String testUrl = mockWebServer.url("/").toString();

            Unirest.post(baseUrl + "/urls")
                    .field("url", testUrl)
                    .asEmpty();

            Url actualUrl = new QUrl()
                    .name.equalTo(testUrl.substring(0, testUrl.length() - 1))
                    .findOne();

            HttpResponse<String> response = Unirest
                    .post(baseUrl + "/urls/" + actualUrl.getId() + "/checks")
                    .asString();

            assertThat(response.getHeaders().getFirst("Location")).isEqualTo("/urls/" + actualUrl.getId());

            String actualBody = Unirest
                    .get(baseUrl + "/urls/" + actualUrl.getId())
                    .asString()
                    .getBody();

            assertThat(actualBody).contains(expectTitle);
            assertThat(actualBody).contains(expectH1);
            assertThat(actualBody).contains(expectDescription);
        }

        @Test
        void deleteUrl() {
            String testValue = "https://test.com";

            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", testValue)
                    .asEmpty();

            Url testUrl = new QUrl()
                    .name.equalTo(testValue)
                    .findOne();

            HttpResponse<String> responseDelete = Unirest
                    .post(baseUrl + "/urls/" + testUrl.getId() + "/delete")
                    .asEmpty();

            assertThat(responseDelete.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();

            assertThat(response.getBody()).doesNotContain(testValue);
            assertThat(response.getBody()).contains("Страница удалена");

            Boolean noneUrl = new QUrl()
                    .name.equalTo(testValue)
                    .exists();

            assertThat(noneUrl).isFalse();
        }
    }
}
