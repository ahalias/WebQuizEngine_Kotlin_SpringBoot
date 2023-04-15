package engine

import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.*
import javax.validation.Valid



@RestController
class QuizQuestions(
    private val quizRepository: QuizRepository,
    private val userRepo: UserRepository,
    private val encoder: PasswordEncoder,
    private val dbmanagement: DBManagement
) {

    @PostMapping("/api/register")
    fun register(@RequestBody @Valid user: User): ResponseEntity<QuizResponse> {
        if (userRepo.findByEmail(user.email) != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(QuizResponse(false, "Failed to register user"))
        }
        try {
            dbmanagement.registerUser(user, encoder)
            return ResponseEntity.status(HttpStatus.OK).body(QuizResponse(true, "User registered successfully ${user.email}, ${user.password}"))
        } catch(e: Exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(QuizResponse(false, "Failed to register user: ${e.message}"))
            }
    }

    @PostMapping("/api/quizzes")
    fun addQuiz(@RequestBody @Valid quizbody: QuizBody, authentication: Authentication): QuizBody = dbmanagement.saveUserToDb(authentication, quizbody)

    @GetMapping("/api/quizzes")
    fun getQuizzes(@RequestParam page: Int): Page<QuizBody> = dbmanagement.findAllQuizzes(page)


    @GetMapping("api/quizzes/{id}")
    fun resolveQuizById(@PathVariable id: Long): QuizBody = dbmanagement.findQuizById(id)

    @DeleteMapping("api/quizzes/{id}")
    fun deleteQuiz(@PathVariable id: Long, authentication: Authentication): ResponseEntity<QuizResponse> {
        dbmanagement.deleteQuiz(id, authentication)
        dbmanagement.deleteCompletedQuiz(id)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(QuizResponse(true, "Quiz successfully deleted"))
    }

    @PostMapping("/api/quizzes/{id}/solve")
    fun solveQuiz(@PathVariable id: Long, authentication: Authentication, @RequestBody answer: Map<String, Array<Int>>): QuizResponse {
        val quiz = quizRepository.findById(id).orElseThrow { QuizNotFound("Quiz not found") }
            return if (quiz.answer.contentEquals(answer["answer"]) || answer["answer"].isNullOrEmpty() && quiz.answer.isEmpty()) {
                dbmanagement.addQuizToSaved(authentication, quiz)
                QuizResponse( true, "Congratulations, you're right!")
            } else  QuizResponse(false,"Wrong answer! Please, try again.")
    }

    @GetMapping("/api/quizzes/completed")
    fun resolveCompletedQuizzes(@RequestParam page: Int, authentication: Authentication): Page<CompletedQuizzes>? = dbmanagement.findCompletedQuizzesByAuthor(page, authentication)
}

