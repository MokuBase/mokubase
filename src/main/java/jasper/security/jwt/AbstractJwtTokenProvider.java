package jasper.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jasper.config.Props;
import jasper.domain.User;
import jasper.domain.proj.Tag;
import jasper.management.SecurityMetersService;
import jasper.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static jasper.repository.spec.QualifiedTag.selector;
import static jasper.security.Auth.USER_TAG_HEADER;
import static jasper.security.Auth.getHeader;
import static jasper.security.AuthoritiesConstants.ADMIN;
import static jasper.security.AuthoritiesConstants.MOD;
import static jasper.security.AuthoritiesConstants.PRIVATE;
import static jasper.security.AuthoritiesConstants.SA;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public abstract class AbstractJwtTokenProvider extends AbstractTokenProvider implements TokenProvider {
	private final Logger logger = LoggerFactory.getLogger(AbstractJwtTokenProvider.class);

	private static final String INVALID_JWT_TOKEN = "Invalid JWT token.";

	private static final String[] ROOT_ROLES_ALLOWED = new String[]{ MOD, ADMIN, SA };

	JwtParser jwtParser;

	private final SecurityMetersService securityMetersService;

	AbstractJwtTokenProvider(Props props, UserRepository userRepository, SecurityMetersService securityMetersService) {
		super(props, userRepository);
		this.securityMetersService = securityMetersService;
	}

	Collection<? extends GrantedAuthority> getAuthorities(Claims claims, User user) {
		var auth = getPartialAuthorities(claims);
		if (user != null && User.ROLES.contains(user.getRole())) {
			auth.add(new SimpleGrantedAuthority(user.getRole()));
		}
		return auth;
	}

	List<SimpleGrantedAuthority> getPartialAuthorities(Claims claims) {
		var auth = getPartialAuthorities();
		var authClaim = claims.get(props.getAuthoritiesClaim(), String.class);
		if (isNotBlank(authClaim)) {
			Arrays.stream(authClaim.split(",")).forEach(r -> auth.add(new SimpleGrantedAuthority(r)));
		}
		return auth;
	}

	String getUsername(Claims claims) {
		if (props.isAllowUserTagHeader() && !isBlank(getHeader(USER_TAG_HEADER))) {
			return getHeader(USER_TAG_HEADER);
		}
		var principal = claims.get(props.getUsernameClaim(), String.class);
		logger.debug("Principal: {}", principal);
		var authorities = getPartialAuthorities(claims);
		if (isBlank(principal) ||
			!principal.matches(Tag.QTAG_REGEX) ||
			principal.equals("+user") ||
			principal.equals("_user")) {
			if (authorities.stream().noneMatch(a ->
				Arrays.stream(ROOT_ROLES_ALLOWED).anyMatch(r -> a.getAuthority().equals(r)))) {
				// Invalid username and can't fall back to root user
				logger.debug("Root role not allowed.");
				return null;
			}
			var origin = "";
			if (!isBlank(principal)) {
				try {
					origin = selector(principal).origin;
				} catch (UnsupportedOperationException e) {
					// Invalid origin
					logger.debug("Root role not allowed with origin {}.", origin);
					return null;
				}
			}
			// The root user has access to every other user.
			// Only assign to mods or higher when username is missing.
			if (isBlank(principal) && !principal.equals("+user") && !principal.equals("+user" + origin)) {
				// Default to private user if +user is not exactly specified
				principal = "_user" + origin;
			}
			logger.debug("Username: {}", principal);
			return principal;
		} else if (principal.startsWith("+user/") || principal.startsWith("_user/")) {
			return principal;
		}
		var isPrivate = authorities.stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals(PRIVATE));
		logger.debug("Username: {}", (isPrivate ? "_" : "+") + principal);
		return (isPrivate ? "_user/" : "+user/") + principal;
	}

	@Override
	public boolean validateToken(String authToken) {
		if (!StringUtils.hasText(authToken)) return false;
		try {
			var claims = jwtParser.parseClaimsJws(authToken).getBody();
			if (!props.getSecurity().getAuthentication().getJwt().getClientId().equals(claims.getAudience())) {
				this.securityMetersService.trackTokenInvalidAudience();
				logger.trace(INVALID_JWT_TOKEN + " Invalid Audience");
			} else {
				return true;
			}
		} catch (ExpiredJwtException e) {
			securityMetersService.trackTokenExpired();
			logger.trace(INVALID_JWT_TOKEN, e);
		} catch (UnsupportedJwtException e) {
			securityMetersService.trackTokenUnsupported();
			logger.trace(INVALID_JWT_TOKEN, e);
		} catch (MalformedJwtException e) {
			securityMetersService.trackTokenMalformed();
			logger.trace(INVALID_JWT_TOKEN, e);
		} catch (SignatureException e) {
			securityMetersService.trackTokenInvalidSignature();
			logger.trace(INVALID_JWT_TOKEN, e);
		} catch (IllegalArgumentException e) {
			logger.error("Token validation error {}", e.getMessage());
		}
		return false;
	}
}
