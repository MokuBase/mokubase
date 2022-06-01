package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ModifiedException extends RuntimeException {
	public ModifiedException() {
		super("Already modified");
	}
}
