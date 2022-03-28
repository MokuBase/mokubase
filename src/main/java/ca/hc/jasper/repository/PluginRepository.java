package ca.hc.jasper.repository;

import ca.hc.jasper.domain.Plugin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PluginRepository extends JpaRepository<Plugin, String>, JpaSpecificationExecutor<Plugin> {
}
