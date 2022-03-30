package ca.hc.jasper.repository;

import java.util.Optional;

import ca.hc.jasper.domain.proj.HasTags;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RefMixin<T extends HasTags> extends JpaSpecificationExecutor<T> {
	Optional<T> findOneByUrlAndOrigin(String url, String origin);
	void deleteByUrlAndOrigin(String url, String origin);
	boolean existsByUrlAndOrigin(String url, String origin);
}
