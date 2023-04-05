package jasper.security.jwt;

import jasper.config.Props;
import jasper.domain.User;
import jasper.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static jasper.security.Auth.USER_ROLE_HEADER;
import static jasper.security.Auth.getHeader;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public abstract class AbstractTokenProvider implements TokenProvider {
	private final Logger logger = LoggerFactory.getLogger(AbstractTokenProvider.class);

	public Props props;

	public UserRepository userRepository;

	AbstractTokenProvider(Props props, UserRepository userRepository) {
		this.props = props;
		this.userRepository = userRepository;
	}

	User getUser(String userTag) {
		if (userRepository == null) return null;
		return userRepository.findOneByQualifiedTag(userTag).orElse(null);
	}

	Collection<? extends GrantedAuthority> getAuthorities(User user) {
		var auth = getPartialAuthorities();
		if (user != null && User.ROLES.contains(user.getRole())) {
			auth.add(new SimpleGrantedAuthority(user.getRole()));
		}
		return auth;
	}

	List<SimpleGrantedAuthority> getPartialAuthorities() {
		var authString = props.getDefaultRole();
		if (props.isAllowUserRoleHeader() && isNotBlank(getHeader(USER_ROLE_HEADER))) {
			authString += getHeader(USER_ROLE_HEADER);
		}
		return Arrays
			.stream(authString.split(","))
			.filter(roles -> !roles.trim().isEmpty())
			.map(SimpleGrantedAuthority::new)
			.collect(Collectors.toList());
	}
}
