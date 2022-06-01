package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateTagException extends RuntimeException {
	public DuplicateTagException() {
		super("Duplicate tag");
	}
}
