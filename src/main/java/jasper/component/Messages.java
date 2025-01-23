package jasper.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jasper.component.dto.ComponentDtoMapper;
import jasper.domain.Ext;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.domain.Template;
import jasper.domain.User;
import jasper.domain.proj.HasTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static jasper.component.Replicator.deletorTag;
import static jasper.domain.Ref.getHierarchicalTags;
import static jasper.domain.proj.HasOrigin.formatOrigin;
import static jasper.domain.proj.HasTags.formatTag;
import static jasper.domain.proj.Tag.localTag;
import static jasper.domain.proj.Tag.tagOrigin;
import static org.springframework.messaging.support.MessageBuilder.createMessage;

@Component
public class Messages {
	private final Logger logger = LoggerFactory.getLogger(Messages.class);

	@Qualifier("taskScheduler")
	@Autowired
	TaskExecutor taskExecutor;

	@Autowired
	MessageChannel cursorTxChannel;

	@Autowired
	MessageChannel refTxChannel;

	@Autowired
	MessageChannel tagTxChannel;

	@Autowired
	MessageChannel responseTxChannel;

	@Autowired
	MessageChannel userTxChannel;

	@Autowired
	MessageChannel extTxChannel;

	@Autowired
	MessageChannel pluginTxChannel;

	@Autowired
	MessageChannel templateTxChannel;

	@Autowired
	ComponentDtoMapper mapper;

	@Autowired
	ObjectMapper objectMapper;

	@Async
	public void updateRef(Ref ref) {
		// TODO: Debounce
		var update = mapper.domainToDto(ref);
		sendAndRetry(() -> refTxChannel.send(createMessage(update, refHeaders(ref.getOrigin(), update))));
		if (update.getTags() != null) {
			for (var tag : update.getTags()) {
				for (var path : getHierarchicalTags(List.of(tag))) {
					sendAndRetry(() -> tagTxChannel.send(createMessage(tag, tagHeaders(ref.getOrigin(), path))));
				}
			}
		}
		if (ref.getSources() != null) {
			for (var source : ref.getSources()) {
				if (source.equals(ref.getUrl())) continue;
				sendAndRetry(() -> responseTxChannel.send(createMessage(ref.getUrl(), responseHeaders(ref.getOrigin(), source))));
			}
		}
		sendAndRetry(() -> cursorTxChannel.send(createMessage(ref.getModified(), originHeaders(ref.getOrigin()))));
	}

	@Async
	public void deleteRef(Ref ref) {
		updateRef(deleteNotice(ref));
	}

	@Async
	public void updateExt(Ext ext) {
		var update = mapper.domainToDto(ext);
		sendAndRetry(() -> extTxChannel.send(createMessage(update, tagHeaders(ext.getOrigin(), ext.getTag()))));
		sendAndRetry(() -> cursorTxChannel.send(createMessage(ext.getModified(), originHeaders(ext.getOrigin()))));
	}

	@Async
	public void updateUser(User user) {
		var update = mapper.domainToDto(user);
		sendAndRetry(() -> userTxChannel.send(createMessage(update, tagHeaders(user.getOrigin(), user.getTag()))));
		sendAndRetry(() -> cursorTxChannel.send(createMessage(user.getModified(), originHeaders(user.getOrigin()))));
	}

	@Async
	public void deleteUser(String qualifiedTag) {
		var tag = localTag(qualifiedTag);
		var origin = tagOrigin(qualifiedTag);
		sendAndRetry(() -> userTxChannel.send(createMessage(deleteNotice(tag, origin), tagHeaders(origin, tag))));
	}

	@Async
	public void updatePlugin(Plugin plugin) {
		var update = mapper.domainToDto(plugin);
		sendAndRetry(() -> pluginTxChannel.send(createMessage(update, tagHeaders(plugin.getOrigin(), plugin.getTag()))));
		sendAndRetry(() -> cursorTxChannel.send(createMessage(plugin.getModified(), originHeaders(plugin.getOrigin()))));
	}

	@Async
	public void deletePlugin(String qualifiedTag) {
		var tag = localTag(qualifiedTag);
		var origin = tagOrigin(qualifiedTag);
		sendAndRetry(() -> pluginTxChannel.send(createMessage(deleteNotice(tag, origin), tagHeaders(origin, tag))));
	}

	@Async
	public void updateTemplate(Template template) {
		var update = mapper.domainToDto(template);
		sendAndRetry(() -> templateTxChannel.send(createMessage(update, tagHeaders(template.getOrigin(), template.getTag()))));
		sendAndRetry(() -> cursorTxChannel.send(createMessage(template.getModified(), originHeaders(template.getOrigin()))));
	}

	@Async
	public void deleteTemplate(String qualifiedTag) {
		var tag = localTag(qualifiedTag);
		var origin = tagOrigin(qualifiedTag);
		sendAndRetry(() -> templateTxChannel.send(createMessage(deleteNotice(tag, origin), tagHeaders(origin, tag))));
	}

	private ObjectNode deleteNotice(String tag, String origin) {
		return objectMapper.convertValue(Map.of(
			"tag", deletorTag(tag),
			"origin", origin
		), ObjectNode.class);
	}

	private Ref deleteNotice(Ref ref) {
		return objectMapper.convertValue(Map.of(
			"url", ref.getUrl(),
			"origin", ref.getOrigin(),
			"tags", List.of("internal", "plugin/delete")
		), Ref.class);
	}

	private void sendAndRetry(Runnable fn) {
		try {
			fn.run();
		} catch (MessageDeliveryException e) {
			// TODO: give up after 3 tries
			taskExecutor.execute(() -> sendAndRetry(fn));
		}
	}

	public static MessageHeaders originHeaders(String origin) {
		return new MessageHeaders(Map.of(
			"origin", formatOrigin(origin)
		));
	}

	public static MessageHeaders tagHeaders(String origin, String tag) {
		return new MessageHeaders(Map.of(
			"origin", formatOrigin(origin),
			"tag", formatTag(tag)
		));
	}

	public static MessageHeaders refHeaders(String origin, HasTags ref) {
		return new MessageHeaders(Map.of(
			"origin", formatOrigin(origin),
			"url", ref.getUrl()
		));
	}

	public static MessageHeaders responseHeaders(String origin, String source) {
		return new MessageHeaders(Map.of(
			"origin", formatOrigin(origin),
			"response", source
		));
	}
}
