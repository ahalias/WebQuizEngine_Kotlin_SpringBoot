package engine

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_FOUND)
class QuizNotFound(cause: String) : RuntimeException(cause)

@ResponseStatus(code = HttpStatus.FORBIDDEN)
class Forbidden(cause: String) : RuntimeException(cause)