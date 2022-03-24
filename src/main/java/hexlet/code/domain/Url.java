package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
public class Url extends Model {

    @Id
    private long id;

    private String name;

    @WhenCreated
    private Instant createdAt;

    @OneToMany
    private List<UrlCheck> urlChecks;

    public Url(String name) {
        this.name = name;
    }

    //возвращает время последней проверки
    public Instant getLastCheck() {
        return urlChecks.get(urlChecks.size() - 1).getCreatedAt();
    }
}
