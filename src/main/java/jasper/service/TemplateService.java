package jasper.service;

import io.micrometer.core.annotation.Counted;
import jasper.domain.Template;
import jasper.errors.AlreadyExistsException;
import jasper.errors.DuplicateModifiedDateException;
import jasper.errors.ForeignWriteException;
import jasper.errors.ModifiedException;
import jasper.errors.NotFoundException;
import jasper.repository.TemplateRepository;
import jasper.repository.filter.TemplateFilter;
import jasper.security.Auth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TemplateService {

	@Autowired
	TemplateRepository templateRepository;

	@Autowired
	Auth auth;

	@PreAuthorize("hasRole('ADMIN')")
	@Counted(value = "jasper.service", extraTags = {"service", "template", "method", "create"})
	public void create(Template template) {
		if (!auth.local(template.getOrigin())) throw new ForeignWriteException(template.getOrigin());
		if (templateRepository.existsByQualifiedTag(template.getQualifiedTag())) throw new AlreadyExistsException();
		template.setModified(Instant.now());
		try {
			templateRepository.save(template);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateModifiedDateException();
		}
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.canReadTag(#qualifiedTag)")
	@Counted(value = "jasper.service", extraTags = {"service", "template", "method", "get"})
	public Template get(String qualifiedTag) {
		return templateRepository.findOneByQualifiedTag(qualifiedTag)
								 .orElseThrow(() -> new NotFoundException("Template " + qualifiedTag));
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.canReadQuery(#filter)")
	@Counted(value = "jasper.service", extraTags = {"service", "template", "method", "page"})
	public Page<Template> page(TemplateFilter filter, Pageable pageable) {
		return templateRepository.findAll(filter.spec(), pageable);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@Counted(value = "jasper.service", extraTags = {"service", "template", "method", "update"})
	public void update(Template template) {
		var maybeExisting = templateRepository.findOneByQualifiedTag(template.getQualifiedTag());
		if (maybeExisting.isEmpty()) throw new NotFoundException("Template "+ template.getQualifiedTag());
		var existing = maybeExisting.get();
		if (!template.getModified().truncatedTo(ChronoUnit.SECONDS).equals(existing.getModified().truncatedTo(ChronoUnit.SECONDS))) throw new ModifiedException("Template");
		template.setModified(Instant.now());
		try {
			templateRepository.save(template);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateModifiedDateException();
		}
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	@Counted(value = "jasper.service", extraTags = {"service", "template", "method", "delete"})
	public void delete(String qualifiedTag) {
		try {
			templateRepository.deleteByQualifiedTag(qualifiedTag);
		} catch (EmptyResultDataAccessException e) {
			// Delete is idempotent
		}
	}
}
