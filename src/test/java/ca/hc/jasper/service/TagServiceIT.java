package ca.hc.jasper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;

import ca.hc.jasper.IntegrationTest;
import ca.hc.jasper.domain.*;
import ca.hc.jasper.repository.*;
import ca.hc.jasper.repository.filter.TagFilter;
import ca.hc.jasper.service.errors.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@WithMockUser("tester")
@IntegrationTest
@Transactional
public class TagServiceIT {

	@Autowired
	TagService tagService;

	@Autowired
	TagRepository tagRepository;

	@Autowired
	TemplateRepository templateRepository;

	@Autowired
	UserRepository userRepository;

	@Test
	void testCreateTag() {
		var tag = new Tag();
		tag.setTag("custom");
		tag.setName("Custom");

		assertThatThrownBy(() -> tagService.create(tag))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void testCreateUserTag() {
		var tag = new Tag();
		tag.setTag("user/tester");
		tag.setName("Custom");

		tagService.create(tag);

		var fetched = tagService.get("user/tester", "");
		assertThat(fetched.getTag())
			.isEqualTo("user/tester");
		assertThat(fetched.getName())
			.isEqualTo("Custom");
	}

	@Test
	@WithMockUser(value = "tester", roles = "MOD")
	void testModCreateTag() {
		var tag = new Tag();
		tag.setTag("custom");
		tag.setName("Custom");

		tagService.create(tag);

		var fetched = tagService.get("custom", "");
		assertThat(fetched.getTag())
			.isEqualTo("custom");
		assertThat(fetched.getName())
			.isEqualTo("Custom");
	}

	@Test
	void testReadNonExistentTag() {
		assertThatThrownBy(() -> tagService.get("custom", ""))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void testReadNonExistentPrivateTag() {
		assertThatThrownBy(() -> tagService.get("_secret", ""))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void testReadPublicTag() {
		var tag = new Tag();
		tag.setTag("public");
		tag.setName("Custom");
		tagRepository.save(tag);

		var fetched = tagService.get("public", "");

		assertThat(fetched.getTag())
			.isEqualTo("public");
		assertThat(fetched.getName())
			.isEqualTo("Custom");
	}

	@Test
	void testReadUserTag() {
		var tag = new Tag();
		tag.setTag("user/tester");
		tag.setName("Custom");
		tagRepository.save(tag);

		var fetched = tagService.get("user/tester", "");

		assertThat(fetched.getTag())
			.isEqualTo("user/tester");
		assertThat(fetched.getName())
			.isEqualTo("Custom");
	}

	@Test
	void testReadTag() {
		var tag = new Tag();
		tag.setTag("custom");
		tag.setName("Custom");
		tagRepository.save(tag);

		var fetched = tagService.get("custom", "");

		assertThat(fetched.getTag())
			.isEqualTo("custom");
		assertThat(fetched.getName())
			.isEqualTo("Custom");
	}

	@Test
	void testReadPrivateTagFailed() {
		var tag = new Tag();
		tag.setTag("_secret");
		tag.setName("Secret");
		tagRepository.save(tag);

		assertThatThrownBy(() -> tagService.get("_secret", ""))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void testReadPrivateTag() {
		var user = new User();
		user.setTag("user/tester");
		user.setReadAccess(List.of("_secret"));
		userRepository.save(user);
		var tag = new Tag();
		tag.setTag("_secret");
		tag.setName("Secret");
		tagRepository.save(tag);

		var fetched = tagService.get("_secret", "");

		assertThat(fetched.getTag())
			.isEqualTo("_secret");
		assertThat(fetched.getName())
			.isEqualTo("Secret");
	}

	@Test
	@WithMockUser(value = "tester", roles = {"USER", "PRIVATE"})
	void testReadPrivateUserTag() {
		var tag = new Tag();
		tag.setTag("_user/tester");
		tag.setName("Secret");
		tagRepository.save(tag);

		var fetched = tagService.get("_user/tester", "");

		assertThat(fetched.getTag())
			.isEqualTo("_user/tester");
		assertThat(fetched.getName())
			.isEqualTo("Secret");
	}

	@Test
	void testReadPrivateUserTagFailed() {
		var tag = new Tag();
		tag.setTag("_user/other");
		tag.setName("Secret");
		tagRepository.save(tag);

		assertThatThrownBy(() -> tagService.get("_user/other", ""))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void testPagePublicTag() {
		var tag = new Tag();
		tag.setTag("custom");
		tag.setName("Custom");
		tagRepository.save(tag);

		var page = tagService.page(
			TagFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
		assertThat(page.getContent().get(0).getTag())
			.isEqualTo("custom");
		assertThat(page.getContent().get(0).getName())
			.isEqualTo("Custom");
	}

	@Test
	void testPagePrivateTagHidden() {
		var tag = new Tag();
		tag.setTag("_secret");
		tag.setName("Secret");
		tagRepository.save(tag);

		var page = tagService.page(
			TagFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	void testPagePrivateTag() {
		var user = new User();
		user.setTag("user/tester");
		user.setReadAccess(List.of("_secret"));
		userRepository.save(user);
		var tag = new Tag();
		tag.setTag("_secret");
		tag.setName("Secret");
		tagRepository.save(tag);

		var page = tagService.page(
			TagFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
		assertThat(page.getContent().get(0).getTag())
			.isEqualTo("_secret");
		assertThat(page.getContent().get(0).getName())
			.isEqualTo("Secret");
	}

	@Test
	@WithMockUser(value = "tester", roles = {"USER", "PRIVATE"})
	void testPagePrivateUserTag() {
		var tag = new Tag();
		tag.setTag("_user/tester");
		tag.setName("Secret");
		tagRepository.save(tag);

		var page = tagService.page(
			TagFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
		assertThat(page.getContent().get(0).getTag())
			.isEqualTo("_user/tester");
		assertThat(page.getContent().get(0).getName())
			.isEqualTo("Secret");
	}

	@Test
	@WithMockUser(value = "tester", roles = {"USER", "PRIVATE"})
	void testPagePrivateUserTagFailed() {
		var tag = new Tag();
		tag.setTag("_user/other");
		tag.setName("Secret");
		tagRepository.save(tag);

		var page = tagService.page(
			TagFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	void testPageUserTag() {
		var tag = new Tag();
		tag.setTag("user/tester");
		tag.setName("Secret");
		tagRepository.save(tag);

		var page = tagService.page(
			TagFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
		assertThat(page.getContent().get(0).getTag())
			.isEqualTo("user/tester");
		assertThat(page.getContent().get(0).getName())
			.isEqualTo("Secret");
	}

	@Test
	void testUpdateTag() {
		var user = new User();
		user.setTag("user/tester");
		user.setWriteAccess(List.of("custom"));
		userRepository.save(user);
		var tag = new Tag();
		tag.setTag("custom");
		tag.setName("First");
		tagRepository.save(tag);
		var updated = new Tag();
		updated.setTag("custom");
		updated.setName("Second");
		updated.setModified(tag.getModified());

		tagService.update(updated);

		var fetched = tagService.get("custom", "");
		assertThat(fetched.getTag())
			.isEqualTo("custom");
		assertThat(fetched.getName())
			.isEqualTo("Second");
	}

	@Test
	void testUpdateTagFailed() {
		var tag = new Tag();
		tag.setTag("custom");
		tag.setName("First");
		tagRepository.save(tag);
		var updated = new Tag();
		updated.setTag("custom");
		updated.setName("Second");
		updated.setModified(tag.getModified());

		assertThatThrownBy(() -> tagService.update(updated))
			.isInstanceOf(AccessDeniedException.class);

		var fetched = tagService.get("custom", "");
		assertThat(fetched.getTag())
			.isEqualTo("custom");
		assertThat(fetched.getName())
			.isEqualTo("First");
	}

	@Test
	void testUpdateUserTag() {
		var tag = new Tag();
		tag.setTag("user/tester");
		tag.setName("First");
		tagRepository.save(tag);
		var updated = new Tag();
		updated.setTag("user/tester");
		updated.setName("Second");
		updated.setModified(tag.getModified());

		tagService.update(updated);

		var fetched = tagService.get("user/tester", "");
		assertThat(fetched.getTag())
			.isEqualTo("user/tester");
		assertThat(fetched.getName())
			.isEqualTo("Second");
	}

	@Test
	void testUpdateUserTagFailed() {
		var tag = new Tag();
		tag.setTag("user/other");
		tag.setName("First");
		tagRepository.save(tag);
		var updated = new Tag();
		updated.setTag("user/other");
		updated.setName("Second");
		updated.setModified(tag.getModified());

		assertThatThrownBy(() -> tagService.update(updated))
			.isInstanceOf(AccessDeniedException.class);

		var fetched = tagService.get("user/other", "");
		assertThat(fetched.getTag())
			.isEqualTo("user/other");
		assertThat(fetched.getName())
			.isEqualTo("First");
	}

	@Test
	void testUpdatePrivateTag() {
		var user = new User();
		user.setTag("user/tester");
		user.setReadAccess(List.of("_secret"));
		user.setWriteAccess(List.of("_secret"));
		userRepository.save(user);
		var tag = new Tag();
		tag.setTag("_secret");
		tag.setName("First");
		tagRepository.save(tag);
		var updated = new Tag();
		updated.setTag("_secret");
		updated.setName("Second");
		updated.setModified(tag.getModified());

		tagService.update(updated);

		var fetched = tagService.get("_secret", "");
		assertThat(fetched.getTag())
			.isEqualTo("_secret");
		assertThat(fetched.getName())
			.isEqualTo("Second");
	}

	@Test
	void testUpdatePrivateTagFailed() {
		var user = new User();
		user.setTag("user/tester");
		user.setReadAccess(List.of("_secret"));
		userRepository.save(user);
		var tag = new Tag();
		tag.setTag("_secret");
		tag.setName("First");
		tagRepository.save(tag);
		var updated = new Tag();
		updated.setTag("_secret");
		updated.setName("Second");
		updated.setModified(tag.getModified());

		assertThatThrownBy(() -> tagService.update(updated))
			.isInstanceOf(AccessDeniedException.class);

		var fetched = tagService.get("_secret", "");
		assertThat(fetched.getTag())
			.isEqualTo("_secret");
		assertThat(fetched.getName())
			.isEqualTo("First");
	}

	@Test
	void testUpdatePublicTagFailed() {
		var tag = new Tag();
		tag.setTag("public");
		tag.setName("First");
		tagRepository.save(tag);
		var updated = new Tag();
		updated.setTag("public");
		updated.setName("Second");
		updated.setModified(tag.getModified());

		assertThatThrownBy(() -> tagService.update(updated))
			.isInstanceOf(AccessDeniedException.class);

		var fetched = tagService.get("public", "");
		assertThat(fetched.getTag())
			.isEqualTo("public");
		assertThat(fetched.getName())
			.isEqualTo("First");
	}

	@Test
	void testDeleteTag() {
		var user = new User();
		user.setTag("user/tester");
		user.setWriteAccess(List.of("custom"));
		userRepository.save(user);
		var tag = new Tag();
		tag.setTag("custom");
		tag.setName("First");
		tagRepository.save(tag);

		tagService.delete("custom");

		assertThatThrownBy(() -> tagService.get("custom", ""))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void testDeleteTagFailed() {
		var tag = new Tag();
		tag.setTag("custom");
		tag.setName("First");
		tagRepository.save(tag);

		assertThatThrownBy(() -> tagService.delete("custom"))
			.isInstanceOf(AccessDeniedException.class);

		var fetched = tagService.get("custom", "");
		assertThat(fetched.getTag())
			.isEqualTo("custom");
		assertThat(fetched.getName())
			.isEqualTo("First");
	}

	@Test
	void testDeletePrivateTag() {
		var user = new User();
		user.setTag("user/tester");
		user.setReadAccess(List.of("_secret"));
		user.setWriteAccess(List.of("_secret"));
		userRepository.save(user);
		var tag = new Tag();
		tag.setTag("_secret");
		tag.setName("First");
		tagRepository.save(tag);

		tagService.delete("_secret");

		assertThatThrownBy(() -> tagService.get("_secret", ""))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void testDeletePrivateTagFailed() {
		var user = new User();
		user.setTag("user/tester");
		user.setReadAccess(List.of("_secret"));
		userRepository.save(user);
		var tag = new Tag();
		tag.setTag("_secret");
		tag.setName("First");
		tagRepository.save(tag);

		assertThatThrownBy(() -> tagService.delete("_secret"))
			.isInstanceOf(AccessDeniedException.class);

		var fetched = tagService.get("_secret", "");
		assertThat(fetched.getTag())
			.isEqualTo("_secret");
		assertThat(fetched.getName())
			.isEqualTo("First");
	}

	@Test
	void testValidateTag() {
		var tag = new Tag();
		tag.setTag("user/tester");
		tag.setName("First");

		tagService.validate(tag);
	}

	@Test
	void testValidateTagWithInvalidTemplate() throws IOException {
		var template = new Template();
		template.setTag("user");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template);
		var tag = new Tag();
		tag.setTag("user/tester");
		tag.setName("First");

		assertThatThrownBy(() -> tagService.validate(tag))
			.isInstanceOf(InvalidTemplateException.class);
	}

	@Test
	void testValidateTagWithTemplate() throws IOException {
		var template = new Template();
		template.setTag("user");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template);
		var tag = new Tag();
		tag.setTag("user/tester");
		tag.setName("First");
		tag.setConfig(mapper.readTree("""
		{
			"name": "Alice",
			"age": 100
		}"""));

		tagService.validate(tag);
	}

	@Test
	void testValidatePrivateTag() {
		var user = new User();
		user.setTag("user/tester");
		user.setReadAccess(List.of("_slug/custom"));
		user.setWriteAccess(List.of("_slug/custom"));
		userRepository.save(user);
		var tag = new Tag();
		tag.setTag("_slug/custom");
		tag.setName("First");

		tagService.validate(tag);
	}

	@Test
	void testValidatePrivateTagWithInvalidTemplate() throws IOException {
		var user = new User();
		user.setTag("user/tester");
		user.setReadAccess(List.of("_slug/custom"));
		user.setWriteAccess(List.of("_slug/custom"));
		userRepository.save(user);
		var template = new Template();
		template.setTag("slug");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template);
		var tag = new Tag();
		tag.setTag("_slug/custom");
		tag.setName("First");

		assertThatThrownBy(() -> tagService.validate(tag))
			.isInstanceOf(InvalidTemplateException.class);
	}

	@Test
	void testValidatePrivateTagWithTemplate() throws IOException {
		var user = new User();
		user.setTag("user/tester");
		user.setReadAccess(List.of("_slug/custom"));
		user.setWriteAccess(List.of("_slug/custom"));
		userRepository.save(user);
		var template = new Template();
		template.setTag("slug");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template);
		var tag = new Tag();
		tag.setTag("_slug/custom");
		tag.setName("First");
		tag.setConfig(mapper.readTree("""
		{
			"name": "Alice",
			"age": 100
		}"""));

		tagService.validate(tag);
	}

	@Test
	void testValidatePrivateTagWithInvalidPrivateTemplate() throws IOException {
		var user = new User();
		user.setTag("user/tester");
		user.setReadAccess(List.of("_slug/custom"));
		user.setWriteAccess(List.of("_slug/custom"));
		userRepository.save(user);
		var template = new Template();
		template.setTag("_slug");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template);
		var tag = new Tag();
		tag.setTag("_slug/custom");
		tag.setName("First");

		assertThatThrownBy(() -> tagService.validate(tag))
			.isInstanceOf(InvalidTemplateException.class);
	}

	@Test
	void testValidatePrivateTagWithPrivateTemplate() throws IOException {
		var user = new User();
		user.setTag("user/tester");
		user.setReadAccess(List.of("_slug/custom"));
		user.setWriteAccess(List.of("_slug/custom"));
		userRepository.save(user);
		var template = new Template();
		template.setTag("_slug");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template);
		var tag = new Tag();
		tag.setTag("_slug/custom");
		tag.setName("First");
		tag.setConfig(mapper.readTree("""
		{
			"name": "Alice",
			"age": 100
		}"""));

		tagService.validate(tag);
	}
}
