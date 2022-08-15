package jasper.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import com.vladmihalcea.hibernate.type.search.PostgreSQLTSVectorType;
import jasper.domain.proj.HasOrigin;
import jasper.domain.proj.HasTags;
import jasper.domain.proj.Tag;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static jasper.domain.proj.Tag.TAG_LEN;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Entity
@Getter
@Setter
@IdClass(RefId.class)
@TypeDefs({
	@TypeDef(name = "json", typeClass = JsonType.class),
	@TypeDef(name = "tsvector", typeClass = PostgreSQLTSVectorType.class)
})
public class Ref implements HasTags {
	public static final String REGEX = "^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?";
	public static final int URL_LEN = 4096;
	public static final int TITLE_LEN = 512;
	public static final int SEARCH_LEN = 512;

	@Id
	@Column(updatable = false)
	@NotBlank
	@Pattern(regexp = REGEX)
	@Length(max = URL_LEN)
	private String url;

	@Id
	@Column(updatable = false)
	@Pattern(regexp = HasOrigin.REGEX)
	@Length(max = ORIGIN_LEN)
	private String origin = "";

	@Length(max = TITLE_LEN)
	private String title;

	private String comment;

	@Type(type = "json")
	@Column(columnDefinition = "jsonb")
	private List<@Length(max = TAG_LEN) @Pattern(regexp = Tag.REGEX) String> tags;

	@Type(type = "json")
	@Column(columnDefinition = "jsonb")
	private List<@Length(max = URL_LEN) @Pattern(regexp = REGEX) String> sources;

	@Type(type = "json")
	@Column(columnDefinition = "jsonb")
	private List<@Length(max = URL_LEN) @Pattern(regexp = REGEX) String> alternateUrls;

	@Type(type = "json")
	@Column(columnDefinition = "jsonb")
	private ObjectNode plugins;

	@Type(type = "json")
	@Column(columnDefinition = "jsonb")
	private Metadata metadata;

	@Formula("COALESCE(jsonb_array_length(sources), 0)")
	private String sourceCount;

	@Formula("COALESCE(jsonb_array_length(metadata -> 'responses'), 0)")
	private String responseCount;

	@Formula("COALESCE(jsonb_array_length(metadata -> 'plugins' -> 'plugin/comment'), 0)")
	private String commentCount;

	@Column
	@NotNull
	private Instant published = Instant.now();

	@CreatedDate
	@Column(updatable = false)
	private Instant created = Instant.now();

	@LastModifiedDate
	private Instant modified = Instant.now();

	@Type(type = "tsvector")
	@Column(updatable = false, insertable = false)
	private String textsearchEn;

	public void addHierarchicalTags() {
		if (tags == null) return;
		for (var i = tags.size() - 1; i >= 0; i--) {
			var t = tags.get(i);
			while (t.contains("/")) {
				t = t.substring(0, t.lastIndexOf("/"));
				if (!tags.contains(t)) {
					tags.add(t);
				}
			}
		}
	}

	public void removePrefixTags() {
		removePrefixTags(this.tags);
	}

	@JsonIgnore
	public Ref addTags(List<String> toAdd) {
		if (toAdd == null) return this;
		if (tags == null) {
			tags = toAdd;
		} else {
			for (var t : toAdd) {
				if (t.startsWith("-")) {
					tags.remove(t.substring(1));
				}
				else if (!tags.contains(t)) {
					tags.add(t);
				}
			}
		}
		return this;
	}

	@JsonIgnore
	public List<String> getQualifiedTags() {
		if (isBlank(origin)) return getTags();
		if (getTags() == null) return null;
		return getTags()
			.stream()
			.map(t -> t + getOrigin())
			.toList();
	}

	@JsonIgnore
	public List<String> getQualifiedNonPublicTags() {
		if (getTags() == null) return null;
		return getTags()
			.stream()
			.filter(t -> t.startsWith("_") || t.startsWith("+"))
			.map(t -> t + getOrigin())
			.toList();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Ref ref = (Ref) o;
		return url.equals(ref.url) && origin.equals(ref.origin);
	}

	@Override
	public int hashCode() {
		return Objects.hash(url, origin);
	}

	public static void removePrefixTags(List<String> tags) {
		if (tags == null) return;
		for (int i = tags.size() - 1; i >= 0; i--) {
			var check = tags.get(i) + "/";
			for (int j = 0; j < tags.size(); j++) {
				if (tags.get(j).startsWith(check)) {
					tags.remove(i);
					break;
				}
			}
		}
	}
}
