package ca.hc.jasper.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.*;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

@Entity
@Getter
@Setter
@IdClass(RefId.class)
@TypeDefs({
	@TypeDef(name = "json", typeClass = JsonType.class)
})
public class Ref {

	@Id
	private String url;

	@Id
	private String origin;

	@Type(type = "json")
	@Column(columnDefinition = "jsonb")
	private List<String> sources;

	private String title;

	@Type(type = "json")
	@Column(columnDefinition = "jsonb")
	private List<String> tags;

	private String comment;

	@Type(type = "json")
	@Column(columnDefinition = "jsonb")
	private List<String> alternateUrls;

	private Instant created;

	private Instant modified;

	private Instant published;

	public RefId getId() {
		return new RefId(url, origin);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Ref ref = (Ref) o;
		return url.equals(ref.url) && Objects.equals(origin, ref.origin);
	}

	@Override
	public int hashCode() {
		return Objects.hash(url);
	}
}
