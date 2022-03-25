package ca.hc.jasper.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

@Entity
@Getter
@Setter
@TypeDefs({
	@TypeDef(name = "json", typeClass = JsonType.class)
})
public class Feed {

	@Id
	@NotNull
	private String origin;

	private String name;

	private String proxy;

	@Type(type = "json")
	@Column(columnDefinition = "jsonb")
	private List<String> tags;

	private Instant modified = Instant.now();

	private Instant lastScrape;

	public boolean local() {
		return origin == null || origin.isBlank();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Feed feed = (Feed) o;
		return origin.equals(feed.origin) && name.equals(feed.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(origin, name);
	}
}
