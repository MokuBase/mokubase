package ca.hc.jasper.repository;

import java.util.List;

import ca.hc.jasper.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository<Template, TagId>, QualifiedTagMixin<Template> {

	@Query("""
		FROM Template AS t
		WHERE t.origin = :origin
			AND t.schema IS NOT NULL
			AND (t.tag = ''
				OR locate(concat(t.tag, '/'), :tag) = 1
				OR (:tag LIKE '\\_%' AND locate(concat(t.tag, '/'), :tag) = 2))""")
	List<Template> findAllForTagAndOriginWithSchema(String tag, String origin);
}
