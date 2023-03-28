package jasper.security.jwt;

import jasper.config.Props;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.List;

public class TokenProviderImplDefault implements TokenProvider {

	Props props;

	public TokenProviderImplDefault(Props props) {
		this.props = props;
	}

	@Override
	public boolean validateToken(String jwt) {
		return true;
	}

	@Override
	public Authentication getAuthentication(String jwt) {
		return new PreAuthenticatedAuthenticationToken(props.getDefaultUser(), null, List.of(new SimpleGrantedAuthority(props.getDefaultRole())));
	}
}
