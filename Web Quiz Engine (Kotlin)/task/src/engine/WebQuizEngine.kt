package engine

import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.*
import javax.validation.Valid
import org.springframework.data.domain.PageRequest


@RestController
class QuizQuestions(
    private val quizRepository: QuizRepository,
    val userRepo: UserRepository,
    val completedQuizzesRepository: CompletedQuizzesRepository,
    val encoder: PasswordEncoder,
    val dbmanagement: DBManagement
) {

    @PostMapping("/api/register")
    fun register(@RequestBody @Valid user: User): ResponseEntity<QuizResponse> {
        if (userRepo.findByEmail(user.email) != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(QuizResponse(false, "Failed to register user"))
        }
        try {
            dbmanagement.saveUserToDb(user, encoder)
            return ResponseEntity.status(HttpStatus.OK).body(QuizResponse(true, "User registered successfully ${user.email}, ${user.password}"))
        } catch(e: Exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(QuizResponse(false, "Failed to register user: ${e.message}"))
            }
    }

    @PostMapping("/api/quizzes")
    fun addQuiz(@RequestBody @Valid quizbody: QuizBody, authentication: Authentication): QuizBody {
        val author = userRepo.findByEmail(authentication.name)
        val quiz = QuizBody().apply {
            this.title = quizbody.title
            this.text = quizbody.text
            this.options = quizbody.options
            this.answer = quizbody.answer
            this.author = author
        }
        quizRepository.save(quiz)

        return quiz
    }

    @GetMapping("/api/quizzes")
        fun getQuizzes(@RequestParam page: Int): ResponseEntity<Page<QuizBody>> {
        val quizPage = quizRepository.findAll(PageRequest.of(page, 10))
        return ResponseEntity.ok(quizPage)
    }


    @GetMapping("api/quizzes/{id}")
    fun resolveQuizById(@PathVariable id: Long): QuizBody {
        val quiz = quizRepository.findById(id).orElseThrow { QuizNotFound("Quiz not found") }
        return quiz
    }

    @DeleteMapping("api/quizzes/{id}")
    fun deleteQuiz(@PathVariable id: Long, authentication: Authentication): ResponseEntity<QuizResponse> {
        val quiz = quizRepository.findById(id).orElseThrow { QuizNotFound("Quiz not found") }
        val author = userRepo.findByEmail(authentication.name)
        if (author != quiz.author) throw Forbidden("You are not author") else {
        quizRepository.delete(quiz)
            val completedQuizList = completedQuizzesRepository.findAllById(id)
            completedQuizList.forEach { completedQuizzesRepository.delete(it) }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(QuizResponse(true, "Quiz successfully deleted"))

    }}

    @PostMapping("/api/quizzes/{id}/solve")
    fun solveQuiz(@PathVariable id: Long, authentication: Authentication, @RequestBody answer: Map<String, Array<Int>>): QuizResponse {
        val quiz = quizRepository.findById(id).orElseThrow { QuizNotFound("Quiz not found") }
            return if (quiz.answer.contentEquals(answer["answer"])) {
                dbmanagement.addQuizToSaved(authentication, quiz)
                return QuizResponse( true, "Congratulations, you're right!")

            }
            else if (answer["answer"].isNullOrEmpty() && quiz.answer.isEmpty()) {
                dbmanagement.addQuizToSaved(authentication, quiz)
                return QuizResponse( true, "Congratulations, you're right!")
            } else  QuizResponse(false,"Wrong answer! Please, try again.")
    }

    @GetMapping("/api/quizzes/completed")
    fun resolveCompletedQuizzes(@RequestParam page: Int, authentication: Authentication): ResponseEntity<Page<CompletedQuizzes>> {
        val quizzes: Page<CompletedQuizzes>? = completedQuizzesRepository.findBycompletedByOrderByCompletedAtDesc(authentication.name, PageRequest.of(page, 10))
        return ResponseEntity.status(HttpStatus.OK).body(quizzes)
    }
}

